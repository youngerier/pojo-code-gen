package io.github.youngerier.generator.analysis;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.JavadocComment;

/**
 * AST 节点注释提取工具，支持 Javadoc 和普通注释，无注释时返回空字符串。
 */
final class Comments {

    private Comments() {
    }

    static String extract(Node node) {
        return node.getComment()
                .map(comment -> {
                    if (comment instanceof JavadocComment javadoc) {
                        return javadoc.parse().getDescription().toText().trim();
                    }
                    return cleanLineComment(comment.getContent());
                })
                .orElse("");
    }

    /**
     * 清理普通注释每行前导的 * 号与空白。
     */
    private static String cleanLineComment(String content) {
        StringBuilder sb = new StringBuilder();
        for (String line : content.split("\\R")) {
            String cleaned = line.replaceFirst("^\\s*\\*+\\s?", "").trim();
            if (!cleaned.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(cleaned);
            }
        }
        return sb.toString();
    }
}
