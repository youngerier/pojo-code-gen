package io.github.youngerier.support.trace;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 异步任务 MDC 透传装饰器：提交任务时复制当前 MDC（含 traceId），
 * 在执行线程上恢复，执行完毕后清理，保证异步日志链路连续。
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            if (context != null) {
                MDC.setContextMap(context);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
