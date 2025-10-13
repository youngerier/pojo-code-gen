package io.githhub.youngerier.office.export;

import io.githhub.youngerier.office.ExcelDocumentWriter;
import io.githhub.youngerier.office.OfficeDocumentTaskInfo;
import io.githhub.youngerier.office.OfficeTaskState;
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
            if (Objects.equals(newState, OfficeTaskState.COMPLETED)) {
                writer.finish();
            }
            this.endTime.set(LocalDateTime.now());
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
