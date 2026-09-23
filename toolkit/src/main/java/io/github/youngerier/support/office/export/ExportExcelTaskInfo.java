package io.github.youngerier.support.office.export;

import io.github.youngerier.support.AssertUtils;
import io.github.youngerier.support.office.ExcelDocumentWriter;
import io.github.youngerier.support.office.OfficeDocumentTaskInfo;
import io.github.youngerier.support.office.OfficeTaskState;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;

import java.beans.Transient;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * excel 导出文档任务 Info
 *
 **/
@Getter
@Builder
public class ExportExcelTaskInfo implements OfficeDocumentTaskInfo {

    private final String id;

    private final String name;

    private final AtomicReference<OfficeTaskState> state;

    private final AtomicReference<LocalDateTime> beginTime;

    private final AtomicReference<LocalDateTime> endTime;

    private final AtomicInteger rowSize = new AtomicInteger(0);

    private final AtomicInteger failedRowSize = new AtomicInteger(0);

    /**
     * 一次抓取数据的大小
     */
    private final int fetchSize;

    private final ExcelDocumentWriter writer;


    @Transient
    public ExcelDocumentWriter getWriter() {
        return writer;
    }

    @Override
    public OfficeTaskState getState() {
        return state.get();
    }

    @Override
    public int getRowSize() {
        return rowSize.get();
    }

    @Override
    public int getFailedRowSize() {
        return failedRowSize.get();
    }

    @Override
    public LocalDateTime getBeginTime() {
        return beginTime.get();
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime.get();
    }

    @Override
    public void addRows(Collection<Object> rows) {
        writer.write(rows);
        rowSize.addAndGet(rows.size());
    }

    @Override
    public void addFailedRows(Collection<Object> rows) {
        writer.write(rows);
        failedRowSize.addAndGet(rows.size());
    }

    @Override
    public void updateState(OfficeTaskState newState) {
        if (Objects.equals(newState, OfficeTaskState.EXECUTING)) {
            this.beginTime.set(LocalDateTime.now());
        }
        if (OfficeTaskState.isFinished(newState)) {
            try {
                if (Objects.equals(newState, OfficeTaskState.COMPLETED)) {
                    writer.finish();
                } else {
                    // 失败/中断/取消路径同样必须释放 writer 持有的 workbook 与输出流
                    writer.abort();
                }
            } finally {
                // 即使落盘/释放抛异常，也要推进状态与结束时间，否则任务会永远停在 EXECUTING
                this.endTime.set(LocalDateTime.now());
                this.state.set(newState);
            }
            return;
        }
        this.state.set(newState);
    }

    public static ExportExcelTaskInfo of(String name, ExcelDocumentWriter writer) {
        return of(name, writer, 3000);
    }

    public static ExportExcelTaskInfo of(String name, ExcelDocumentWriter writer, int batchSize) {
        return of(RandomStringUtils.secure().nextAlphanumeric(32), name, writer, batchSize);
    }

    public static ExportExcelTaskInfo of(Object id, String name, ExcelDocumentWriter writer) {
        return of(id, name, writer, 3000);
    }

    public static ExportExcelTaskInfo of(Object id, String name, ExcelDocumentWriter writer, int batchSize) {
        AssertUtils.notNull(writer, "writer must not be null");
        AssertUtils.isTrue(batchSize > 0, "batchSize must be greater than 0, but was {}", batchSize);
        return ExportExcelTaskInfo.builder()
                .id(String.valueOf(id))
                .name(name)
                .beginTime(new AtomicReference<>())
                .endTime(new AtomicReference<>())
                .state(new AtomicReference<>(OfficeTaskState.WAIT))
                .fetchSize(batchSize)
                .writer(writer)
                .build();
    }

}
