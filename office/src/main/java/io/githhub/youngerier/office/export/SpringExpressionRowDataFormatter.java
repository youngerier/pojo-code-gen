package io.githhub.youngerier.office.export;

import io.githhub.youngerier.office.metadata.ExcelCellDescriptor;
import io.githhub.youngerier.office.metadata.ExcelCellPrinter;
import io.github.youngerier.support.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.format.Printer;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Spring Expression 数据格式化
 *
 **/
@AllArgsConstructor
public class SpringExpressionRowDataFormatter {

    @Getter
    private final List<ExcelCellDescriptor> cellDescriptors;

    private static final ExpressionParser PARSER = new SpelExpressionParser();


    public static SpringExpressionRowDataFormatter of(List<ExcelCellDescriptor> cellDescriptors) {
        return new SpringExpressionRowDataFormatter(cellDescriptors);
    }

    public List<String> formatRows(Object row) {
        List<String> result = new ArrayList<>();
        if (row instanceof Collection<?>) {
            // 集合
            return ((Collection<?>) row).stream().map(String::valueOf).collect(java.util.stream.Collectors.toList());
        } else {
            EvaluationContext context = new StandardEvaluationContext(row);
            for (ExcelCellDescriptor descriptor : cellDescriptors) {
                String expression = descriptor.getExpression();
                Object cellValue = StringUtils.hasText(expression) ? PARSER.parseExpression(expression).getValue(context) : row;
                result.add(formatCellValue(descriptor, row, cellValue));
            }
            return result;
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
