package io.githhub.youngerier.office;

import io.github.youngerier.support.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态
 *
 **/
@AllArgsConstructor
@Getter
public enum OfficeTaskState implements DescriptiveEnum {

    WAIT("待执行"),

    EXECUTING("执行中"),

    COMPLETED("处理成功"),

    FAILED("执行失败"),

    INTERRUPT("执行中断"),

    CANCELED("任务取消"),
    ;

    private final String desc;

    public static boolean isFinished(OfficeTaskState state) {
        return state == COMPLETED || state == FAILED || state == INTERRUPT || state == CANCELED;
    }
}
