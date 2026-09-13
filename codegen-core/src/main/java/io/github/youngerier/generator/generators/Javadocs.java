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
     * 追加类注释：存在类注释时，依次追加注释内容与产物标签（无注释则不生成 Javadoc）。
     */
    static void appendClassComment(TypeSpec.Builder builder, ClassMetadata metadata, String label) {
        String comment = metadata.getClassComment();
        if (comment != null && !comment.isEmpty()) {
            builder.addJavadoc(comment + "\n");
            builder.addJavadoc(label + "\n");
        }
    }
}
