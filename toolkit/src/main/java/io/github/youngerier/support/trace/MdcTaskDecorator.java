package io.github.youngerier.support.trace;

import org.springframework.core.task.TaskDecorator;

/**
 * 异步任务 MDC 透传装饰器：委托 {@link TraceContext#wrap(Runnable)}，
 * 让线程池执行的任务继承提交线程的 traceId。
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return TraceContext.wrap(runnable);
    }
}
