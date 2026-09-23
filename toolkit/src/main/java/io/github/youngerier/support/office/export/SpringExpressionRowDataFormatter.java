package io.github.youngerier.support.office.export;

import io.github.youngerier.support.constants.Constants;
import io.github.youngerier.support.exception.BaseException;
import io.github.youngerier.support.exception.DefaultExceptionCode;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import io.github.youngerier.support.office.metadata.ExcelCellPrinter;
import lombok.Getter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingMethodResolver;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.format.Printer;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Spring Expression 数据格式化。
 *
 * <p><b>安全约定</b>：取值表达式属于「模板」内容，可能来自数据库或配置中心，
 * 因此使用只读数据绑定的 {@link SimpleEvaluationContext} 求值，禁止 {@code T(...)} 类型引用、
 * 构造函数与静态方法调用，避免配置写入者获得任意代码执行能力。
 * 表达式在构造时一次性解析并缓存，避免逐行逐列重复解析。
 *
 **/
public class SpringExpressionRowDataFormatter {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    @Getter
    private final List<ExcelCellDescriptor> cellDescriptors;

    /**
     * 与 {@link #cellDescriptors} 按索引一一对应的已解析表达式；{@code null} 表示该列直接使用整行数据
     */
    private final List<Expression> expressions;

    public SpringExpressionRowDataFormatter(List<ExcelCellDescriptor> cellDescriptors) {
        this.cellDescriptors = List.copyOf(cellDescriptors);
        this.expressions = this.cellDescriptors.stream()
                .map(SpringExpressionRowDataFormatter::parseExpression)
                .collect(Collectors.toList());
    }

    public static SpringExpressionRowDataFormatter of(List<ExcelCellDescriptor> cellDescriptors) {
        return new SpringExpressionRowDataFormatter(cellDescriptors);
    }

    public List<String> formatRows(Object row) {
        if (row instanceof Collection<?> collection) {
            // 集合：整行按元素字符串化，不参与表达式取值
            return collection.stream().map(String::valueOf).collect(Collectors.toList());
        }
        EvaluationContext context = evaluationContext();
        List<String> result = new ArrayList<>(cellDescriptors.size());
        for (int i = 0; i < cellDescriptors.size(); i++) {
            ExcelCellDescriptor descriptor = cellDescriptors.get(i);
            Expression expression = expressions.get(i);
            Object cellValue = expression == null ? row : expression.getValue(context, row);
            result.add(formatCellValue(descriptor, row, cellValue));
        }
        return result;
    }

    /**
     * 只读数据绑定上下文：支持属性读取（含 Lombok getter）与普通实例方法调用，
     * 但不提供类型引用（{@code T(...)}）、构造器、静态方法与 Bean 引用的能力。
     *
     * <p>必须使用 {@link DataBindingMethodResolver#forInstanceMethodInvocation()}：
     * {@code SimpleEvaluationContext} 明确拒绝普通的 {@code ReflectiveMethodResolver}。
     */
    private static EvaluationContext evaluationContext() {
        return SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withMethodResolvers(DataBindingMethodResolver.forInstanceMethodInvocation())
                .build();
    }

    private static Expression parseExpression(ExcelCellDescriptor descriptor) {
        String expression = descriptor.getExpression();
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        try {
            return PARSER.parseExpression(expression);
        } catch (ParseException e) {
            // 构建期即失败，避免把配置错误推迟到逐行渲染时才暴露
            throw new BaseException(DefaultExceptionCode.INTERNAL_SERVER_ERROR,
                    "非法的单元格取值表达式: " + expression, e);
        }
    }

    private String formatCellValue(ExcelCellDescriptor descriptor, Object row, Object cellValue) {
        if (cellValue == null) {
            return Constants.EMPTY;
        }
        Printer<Object> printer = descriptor.getPrinter();
        if (printer instanceof ExcelCellPrinter) {
            return ((ExcelCellPrinter<Object>) printer).print(cellValue, descriptor.getExpression(), row, Locale.getDefault());
        }
        return printer.print(cellValue, Locale.getDefault());
    }
}
