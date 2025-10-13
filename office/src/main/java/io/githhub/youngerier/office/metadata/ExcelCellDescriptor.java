package io.githhub.youngerier.office.metadata;

import io.github.youngerier.support.AssertUtils;
import io.github.youngerier.support.i18n.SpringI18nMessageUtils;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * excel cell 描述符
 **/
@Getter
public final class ExcelCellDescriptor {

    /**
     * 列标题
     */
    private final String title;

    /**
     * 取值表达式，如果为 null,则传入整行数据
     * 默认使用 spring expression 表达式
     * {@link org.springframework.expression.spel.standard.SpelExpressionParser}
     */
    private final String expression;

    /**
     * excel cell 属性集合，例如 宽度、对齐方式、格式化等
     *
     * @key class
     */
    private final Map<Class<?>, ExcelCellAttribute<?>> attributes;

    private ExcelCellDescriptor(String title, String expression, Collection<ExcelCellAttribute<?>> attributes) {
        this.title = title;
        this.expression = expression;
        this.attributes = attributes.stream().collect(Collectors.toMap(ExcelCellAttribute::getClass, Function.identity()));
    }

    @NotNull
    public Integer getWidth(int defaultWidth) {
        return findAttribute(CellWidth.class)
                .map(ExcelCellAttribute::getValue)
                .orElse(defaultWidth);
    }

    @NotNull
    public Printer<Object> getPrinter() {
        return findAttribute(CellPrinter.class)
                .map(ExcelCellAttribute::getValue)
                .orElseGet(() -> (value, locale) -> String.valueOf(value));
    }

    @NotNull
    public Parser<Object> getParser() {
        return findAttribute(CellParser.class)
                .map(ExcelCellAttribute::getValue)
                .orElseGet(() -> (value, locale) -> value);
    }

    public boolean hasPrinter() {
        return findAttribute(CellPrinter.class).isPresent();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ExcelCellAttribute<T>> findAttribute(Class<? extends ExcelCellAttribute<T>> clazz) {
        ExcelCellAttribute<T> result = (ExcelCellAttribute<T>) attributes.get(clazz);
        return Optional.ofNullable(result);
    }

    public static ExcelCellDescriptorBuilder builder(String title) {
        return builder(title, null);
    }

    public static ExcelCellDescriptorBuilder builder(String title, String expression) {
        return new ExcelCellDescriptorBuilder(SpringI18nMessageUtils.getMessage(title, title), expression);
    }

    public static ExcelCellDescriptorBuilder withExpression(String expression) {
        return new ExcelCellDescriptorBuilder(null, expression);
    }

    public static ExcelCellDescriptor of(String title, String expression) {
        return builder(title, expression).build();
    }

    public static ExcelCellDescriptor of(String title, String expression, int width) {
        return ExcelCellDescriptor.builder(title, expression)
                .width(width)
                .build();
    }

    public static ExcelCellDescriptor expression(String expression) {
        return withExpression(expression).build();
    }


    @AllArgsConstructor
    public static class ExcelCellDescriptorBuilder {

        private final String title;

        private final String expression;

        private final Collection<ExcelCellAttribute<?>> attributes = new ArrayList<>();

        public ExcelCellDescriptorBuilder width(int width) {
            this.attributes.add(new CellWidth(width));
            return this;
        }

        public <T> ExcelCellDescriptorBuilder printer(Printer<T> printer) {
            AssertUtils.isTrue(attributes.stream().noneMatch(CellPrinter.class::isInstance), "Printer already exists");
            this.attributes.add(new CellPrinter(printer));
            return this;
        }

        public <T> ExcelCellDescriptorBuilder parser(Parser<T> parser) {
            AssertUtils.isTrue(attributes.stream().noneMatch(CellPrinter.class::isInstance), "Parser already exists");
            this.attributes.add(new CellParser(parser));
            return this;
        }

        public ExcelCellDescriptorBuilder attributes(Collection<ExcelCellAttribute<?>> attributes) {
            this.attributes.addAll(attributes);
            return this;
        }

        public ExcelCellDescriptor build() {
            return new ExcelCellDescriptor(title, expression, attributes);
        }
    }

    @AllArgsConstructor
    @Getter
    private static class CellWidth implements ExcelCellAttribute<Integer> {

        private final Integer value;
    }

    @AllArgsConstructor
    @Getter
    @SuppressWarnings({"rawtypes"})
    private static class CellPrinter implements ExcelCellAttribute<Printer<Object>> {

        private final Printer value;
    }

    @AllArgsConstructor
    @Getter
    @SuppressWarnings({"rawtypes"})
    private static class CellParser implements ExcelCellAttribute<Parser<Object>> {

        private final Parser value;
    }

}
