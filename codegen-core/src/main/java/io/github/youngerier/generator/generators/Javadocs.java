package io.github.youngerier.generator.generators;

import com.squareup.javapoet.TypeSpec;
import io.github.youngerier.generator.model.ClassMetadata;

/**
 * Javadoc 构建工具。
 */
final class Javadocs {

    private Javadocs() {
    }

    /**
     * 转义 Javadoc 文本中的 {@code $}：JavaPoet 把 {@code $} 视为格式占位符
     * （{@code $L}/{@code $S} 等），注释里直接出现 {@code $}（例如价格 {@code $5}）
     * 会抛 {@code UnknownFieldTypeException}，统一替换为 {@code $$} 转义。
     */
    static String escapeLiteral(String text) {
        return text == null ? null : text.replace("$", "$$");
    }

    /**
     * 追加类注释：存在类注释时，依次追加注释内容与产物标签（无注释则不生成 Javadoc）。
     */
    static void appendClassComment(TypeSpec.Builder builder, ClassMetadata metadata, String label) {
        String comment = metadata.getClassComment();
        if (comment != null && !comment.isEmpty()) {
            builder.addJavadoc(escapeLiteral(comment) + "\n");
            builder.addJavadoc(escapeLiteral(label) + "\n");
        }
    }
}
