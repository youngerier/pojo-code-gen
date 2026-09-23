package io.github.youngerier.support.office.export;

import io.github.youngerier.support.office.ExcelDocumentWriter;
import io.github.youngerier.support.office.ExportExcelDataFetcher;
import io.github.youngerier.support.office.OfficeTaskState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归：翻页循环的终止条件与取消语义。
 *
 * <ul>
 *     <li>取数实现返回 null 时应在库内给出明确异常，而不是 NPE。</li>
 *     <li>取数实现忽略 page 参数（每页都满）时必须被安全上限中断，不能死循环。</li>
 *     <li>任务已进入终态时不得再取数、也不得把终态覆盖成 COMPLETED。</li>
 * </ul>
 */
class SpringExpressionExportExcelTaskTest {

    private static final class RecordingWriter implements ExcelDocumentWriter {

        private final List<Object> written = new ArrayList<>();
        private int finishCalls;
        private int abortCalls;

        @Override
        public void write(Collection<Object> rows) {
            written.addAll(rows);
        }

        @Override
        public void finish() {
            finishCalls++;
        }

        @Override
        public void abort() {
            abortCalls++;
        }
    }

    private static ExportExcelTaskInfo taskInfo(RecordingWriter writer, int batchSize) {
        return ExportExcelTaskInfo.of("task-id", "导出任务", writer, batchSize);
    }

    @Test
    void stopsPagingWhenFinalPageIsShort() {
        RecordingWriter writer = new RecordingWriter();
        ExportExcelTaskInfo info = taskInfo(writer, 10);
        List<Integer> pagesFetched = new ArrayList<>();
        ExportExcelDataFetcher<String> fetcher = (page, size) -> {
            pagesFetched.add(page);
            return page == 1 ? List.of("a", "b") : List.of();
        };

        new SpringExpressionExportExcelTask(info, fetcher).run();

        assertEquals(List.of(1), pagesFetched);
        assertEquals(2, writer.written.size());
        assertEquals(1, writer.finishCalls);
        assertEquals(OfficeTaskState.COMPLETED, info.getState());
    }

    @Test
    void pagesUntilExhausted() {
        RecordingWriter writer = new RecordingWriter();
        ExportExcelTaskInfo info = taskInfo(writer, 2);
        ExportExcelDataFetcher<String> fetcher = (page, size) ->
                page <= 3 ? List.of("a", "b") : List.of();

        new SpringExpressionExportExcelTask(info, fetcher).run();

        assertEquals(6, writer.written.size());
        assertEquals(OfficeTaskState.COMPLETED, info.getState());
    }

    @Test
    void nullFetchResultIsReportedClearly() {
        RecordingWriter writer = new RecordingWriter();
        ExportExcelTaskInfo info = taskInfo(writer, 10);
        ExportExcelDataFetcher<String> fetcher = (page, size) -> null;

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SpringExpressionExportExcelTask(info, fetcher).run());

        assertTrue(ex.getMessage().contains("must not return null"), ex.getMessage());
        assertEquals(OfficeTaskState.FAILED, info.getState());
        assertEquals(1, writer.abortCalls, "失败路径必须释放 writer");
    }

    /**
     * 取数实现永远返回满页 → 必须在上限处中断，而不是死循环。
     */
    @Test
    void neverShrinkingFetcherIsInterruptedBySafetyLimit() {
        RecordingWriter writer = new RecordingWriter();
        ExportExcelTaskInfo info = taskInfo(writer, 2);
        int[] calls = new int[1];
        ExportExcelDataFetcher<String> fetcher = (page, size) -> {
            calls[0]++;
            return List.of("a", "b");
        };

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SpringExpressionExportExcelTask(info, fetcher).run());

        assertTrue(ex.getMessage().contains("安全上限"), ex.getMessage());
        assertEquals(ExportExcelDataFetcher.MAX_FETCH_PAGES, calls[0]);
    }

    /**
     * 回归：原实现先 fetch + addRows 再判断终态，取消时会多写一整页；
     * 且 run() 会把外部设置的 CANCELED 覆盖成 COMPLETED。
     */
    @Test
    void canceledTaskDoesNotFetchAndKeepsCanceledState() {
        RecordingWriter writer = new RecordingWriter();
        ExportExcelTaskInfo info = taskInfo(writer, 10);
        int[] calls = new int[1];
        ExportExcelDataFetcher<String> fetcher = (page, size) -> {
            calls[0]++;
            return List.of("a");
        };

        new SpringExpressionExportExcelTask(info, fetcher, id -> OfficeTaskState.CANCELED).run();

        assertEquals(0, calls[0], "已取消的任务不得再取数");
        assertTrue(writer.written.isEmpty(), "已取消的任务不得再写入");
        assertEquals(OfficeTaskState.CANCELED, info.getState(), "不得被 run() 覆盖成 COMPLETED");
        assertEquals(1, writer.abortCalls);
        assertEquals(0, writer.finishCalls);
    }
}
