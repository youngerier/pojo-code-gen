package io.github.youngerier.support.office.export;

import io.github.youngerier.support.office.AbstractDelegateDocumentTask;
import io.github.youngerier.support.office.ExportExcelDataFetcher;
import io.github.youngerier.support.office.OfficeTaskState;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;

/**
 * 基于 spring expression 取值 excel 导出任务，不支持列合并等操作
 *
 **/
@Slf4j
public class SpringExpressionExportExcelTask extends AbstractDelegateDocumentTask {

    private final ExportExcelDataFetcher<?> fetcher;

    /**
     * 任务状态同步器，用于在分布式情况下通过 redis（缓存） 、或数据库 同步任务的实时状态
     *
     * @key 任务 id
     * @value 任务状态
     */
    private final Function<String, OfficeTaskState> stateSyncer;

    public SpringExpressionExportExcelTask(ExportExcelTaskInfo taskInfo, ExportExcelDataFetcher<?> fetcher) {
        this(taskInfo, fetcher, id -> null);
    }

    public SpringExpressionExportExcelTask(ExportExcelTaskInfo taskInfo, ExportExcelDataFetcher<?> fetcher,
                                           Function<String, OfficeTaskState> stateSyncer) {
        super(taskInfo);
        this.fetcher = fetcher;
        this.stateSyncer = stateSyncer;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void doTask() {
        int queryPage = 1;
        ExportExcelTaskInfo taskInfo = (ExportExcelTaskInfo) getDelegate();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                OfficeTaskState state = stateSyncer.apply(getId());
                if (state != null) {
                    updateState(state);
                }
                // 终态判断必须放在取数之前：原实现先 fetch + addRows 再判断，
                // 任务被取消时仍会多拉取并写入一整页数据
                if (OfficeTaskState.isFinished(getState())) {
                    log.info("excel task is finished, id = {}, state = {}", getId(), getState());
                    return;
                }
                List rows = fetcher.fetch(queryPage, taskInfo.getFetchSize());
                if (rows == null) {
                    throw new IllegalStateException(
                            "ExportExcelDataFetcher.fetch must not return null, page = " + queryPage);
                }
                this.addRows(rows);
                if (rows.size() < taskInfo.getFetchSize()) {
                    // 处理完成
                    break;
                }
                if (queryPage >= ExportExcelDataFetcher.MAX_FETCH_PAGES) {
                    throw new IllegalStateException("导出行数超过安全上限 " + ExportExcelDataFetcher.MAX_FETCH_PAGES
                            + " 页，请检查 ExportExcelDataFetcher 是否忽略了 page 参数");
                }
                queryPage++;
            }
        } finally {
            if (Thread.currentThread().isInterrupted()) {
                log.info("excel task is interrupted，id = {}, state = {}", getId(), getState());
                taskInfo.updateState(OfficeTaskState.INTERRUPT);
            }
        }
    }

}
