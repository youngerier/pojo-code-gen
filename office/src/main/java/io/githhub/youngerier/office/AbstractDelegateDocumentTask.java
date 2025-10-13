package io.githhub.youngerier.office;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collection;


@Getter
@Slf4j
public abstract class AbstractDelegateDocumentTask implements OfficeDocumentTask {

    private final OfficeDocumentTaskInfo delegate;

    protected AbstractDelegateDocumentTask(OfficeDocumentTaskInfo delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public LocalDateTime getBeginTime() {
        return delegate.getBeginTime();
    }

    @Override
    public LocalDateTime getEndTime() {
        return delegate.getEndTime();
    }

    @Override
    public OfficeTaskState getState() {
        return delegate.getState();
    }

    @Override
    public void updateState(OfficeTaskState newState) {
        delegate.updateState(newState);
    }

    @Override
    public void addRows(Collection<Object> rows) {
        delegate.addRows(rows);
    }

    @Override
    public void addFailedRows(Collection<Object> rows) {
        delegate.addFailedRows(rows);
    }

    @Override
    public int getRowSize() {
        return delegate.getRowSize();
    }

    @Override
    public int getFailedRowSize() {
        return delegate.getFailedRowSize();
    }

    protected abstract void doTask();

    @Override
    public void run() {
        try {
            updateState(OfficeTaskState.EXECUTING);
            doTask();
            updateState(OfficeTaskState.COMPLETED);
        } catch (Throwable throwable) {
            updateState(OfficeTaskState.FAILED);
            throw throwable;
        }
    }
}
