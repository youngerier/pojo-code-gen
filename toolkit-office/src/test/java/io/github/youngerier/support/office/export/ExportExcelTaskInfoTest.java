package io.github.youngerier.support.office.export;

import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.office.ExcelDocumentWriter;
import io.github.youngerier.support.office.OfficeTaskState;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 回归：失败/中断/取消路径必须释放 writer（原实现只在 COMPLETED 时 finish，
 * 其余终态完全不释放 POI workbook 与输出流），以及 fetchSize 的下界校验。
 */
class ExportExcelTaskInfoTest {

    private static final class RecordingWriter implements ExcelDocumentWriter {

        private int writeCalls;
        private int finishCalls;
        private int abortCalls;
        private boolean failOnFinish;

        @Override
        public void write(Collection<Object> rows) {
            writeCalls++;
        }

        @Override
        public void finish() {
            finishCalls++;
            if (failOnFinish) {
                throw new IllegalStateException("finish failed");
            }
        }

        @Override
        public void abort() {
            abortCalls++;
        }
    }

    private static ExportExcelTaskInfo taskInfo(RecordingWriter writer, int batchSize) {
        ExportExcelTaskInfo info = ExportExcelTaskInfo.of("task-id", "导出任务", writer, batchSize);
        assertEquals("task-id", info.getId());
        return info;
    }

    // ---------------- fetchSize / writer 校验 ----------------

    @Test
    void rejectsNonPositiveBatchSize() {
        RecordingWriter writer = new RecordingWriter();

        assertThrows(BaseException.class, () -> ExportExcelTaskInfo.of("id", "n", writer, 0));
        assertThrows(BaseException.class, () -> ExportExcelTaskInfo.of("id", "n", writer, -1));
    }

    @Test
    void rejectsNullWriter() {
        assertThrows(BaseException.class, () -> ExportExcelTaskInfo.of("id", "n", null, 100));
    }

    @Test
    void acceptsPositiveBatchSize() {
        ExportExcelTaskInfo info = taskInfo(new RecordingWriter(), 1);
        assertEquals(1, info.getFetchSize());

        ExportExcelTaskInfo defaulted = ExportExcelTaskInfo.of("id", "n", new RecordingWriter());
        assertEquals(3000, defaulted.getFetchSize());
    }

    // ---------------- 状态机与资源释放 ----------------

    @Test
    void completedFinishesWriterAndRecordsTimes() {
        RecordingWriter writer = new RecordingWriter();
        ExportExcelTaskInfo info = taskInfo(writer, 10);

        info.updateState(OfficeTaskState.EXECUTING);
        assertNotNull(info.getBeginTime());

        info.updateState(OfficeTaskState.COMPLETED);

        assertEquals(1, writer.finishCalls);
        assertEquals(0, writer.abortCalls);
        assertEquals(OfficeTaskState.COMPLETED, info.getState());
        assertNotNull(info.getEndTime());
    }

    @Test
    void failedStateReleasesWriter() {
        RecordingWriter writer = new RecordingWriter();
        ExportExcelTaskInfo info = taskInfo(writer, 10);
        info.updateState(OfficeTaskState.EXECUTING);

        info.updateState(OfficeTaskState.FAILED);

        assertEquals(1, writer.abortCalls, "失败路径必须释放 writer");
        assertEquals(0, writer.finishCalls);
        assertEquals(OfficeTaskState.FAILED, info.getState());
        assertNotNull(info.getEndTime());
    }

    @Test
    void interruptAndCanceledStatesReleaseWriter() {
        RecordingWriter interruptWriter = new RecordingWriter();
        taskInfo(interruptWriter, 10).updateState(OfficeTaskState.INTERRUPT);
        assertEquals(1, interruptWriter.abortCalls);

        RecordingWriter cancelWriter = new RecordingWriter();
        taskInfo(cancelWriter, 10).updateState(OfficeTaskState.CANCELED);
        assertEquals(1, cancelWriter.abortCalls);
    }

    /**
     * 回归：finish 抛异常时，状态与结束时间仍必须推进，否则任务会永远停在 EXECUTING。
     */
    @Test
    void stateAdvancesEvenWhenFinishThrows() {
        RecordingWriter writer = new RecordingWriter();
        writer.failOnFinish = true;
        ExportExcelTaskInfo info = taskInfo(writer, 10);
        info.updateState(OfficeTaskState.EXECUTING);

        assertThrows(IllegalStateException.class, () -> info.updateState(OfficeTaskState.COMPLETED));

        assertEquals(OfficeTaskState.COMPLETED, info.getState());
        assertNotNull(info.getEndTime());
    }

    @Test
    void addRowsDelegatesToWriterAndCounts() {
        RecordingWriter writer = new RecordingWriter();
        ExportExcelTaskInfo info = taskInfo(writer, 10);

        info.addRows(List.of("a", "b"));
        info.addFailedRows(List.of("c"));

        assertEquals(2, writer.writeCalls);
        assertEquals(2, info.getRowSize());
        assertEquals(1, info.getFailedRowSize());
        assertEquals(OfficeTaskState.WAIT, info.getState());
    }
}
