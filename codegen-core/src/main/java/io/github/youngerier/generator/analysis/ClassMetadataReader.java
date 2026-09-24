package io.github.youngerier.generator.analysis;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.github.youngerier.generator.model.ClassMetadata;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 类元数据读取器：把 {@link ModelInput} 转换为 {@link ClassMetadata}。
 *
 * <p>字段包含本类与直接父类（非 Object，仅一层）的实例字段，按出现顺序去重；
 * 字段类型经符号求解后由 {@link TypeMapper} 映射，无法求解时保留源码写法并告警。
 */
@Slf4j
public final class ClassMetadataReader {

    public ClassMetadata read(ModelInput input) {
        ClassMetadata metadata = new ClassMetadata();
        metadata.setPackageName(input.packageName());
        metadata.setClassName(input.type().getNameAsString());
        metadata.setClassComment(Comments.extract(input.type()));

        Set<String> seen = new LinkedHashSet<>();
        addAstFields(input.type(), metadata, seen);
        addDirectParentFields(input.type(), metadata, seen);
        return metadata;
    }

    /**
     * 追加 AST 类声明中的实例字段，跳过静态字段与同名重复字段。
     */
    private void addAstFields(ClassOrInterfaceDeclaration declaration,
                              ClassMetadata metadata, Set<String> seen) {
        for (FieldDeclaration fieldDeclaration : declaration.getFields()) {
            if (fieldDeclaration.isStatic()) {
                continue;
            }
            for (VariableDeclarator variable : fieldDeclaration.getVariables()) {
                if (seen.add(variable.getNameAsString())) {
                    metadata.getFields().add(toFieldInfo(fieldDeclaration, variable));
                }
            }
        }
    }

    private ClassMetadata.FieldInfo toFieldInfo(FieldDeclaration fieldDeclaration,
                                                VariableDeclarator variable) {
        ClassMetadata.FieldInfo fieldInfo = new ClassMetadata.FieldInfo();
        fieldInfo.setName(variable.getNameAsString());

        try {
            ResolvedType resolvedType = variable.getType().resolve();
            fieldInfo.setFullType(resolvedType.describe());
            fieldInfo.setType(TypeMapper.map(resolvedType));
            fieldInfo.setColumn(isMappedColumn(resolvedType));
        } catch (Exception e) {
            // 依赖缺失导致无法求解时，保留源码中的类型写法：先尝试剥离泛型参数生成原始类型
            String writtenType = variable.getTypeAsString();
            log.warn("字段 {} 的类型无法求解，按源码类型 [{}] 原样保留",
                    fieldInfo.getName(), writtenType);
            fieldInfo.setFullType(writtenType);
            fieldInfo.setType(lenientClassName(writtenType));
            // 无法求解时按源码写法兜底识别容器类型
            fieldInfo.setColumn(!isContainerTypeName(writtenType));
        }

        fieldInfo.setComment(Comments.extract(fieldDeclaration));
        // 主键以 @Id 注解决定，而非字段名以 Id 结尾（orderId 不是主键）
        fieldInfo.setPrimaryKey(fieldDeclaration.getAnnotationByName("Id").isPresent());
        return fieldInfo;
    }

    /**
     * 解析直接父类（仅一层）的实例字段，父类无法求解或是 Object 时忽略。
     */
    private void addDirectParentFields(ClassOrInterfaceDeclaration declaration,
                                       ClassMetadata metadata, Set<String> seen) {
        declaration.getExtendedTypes().stream().findFirst().ifPresent(extended -> {
            ResolvedReferenceType parent;
            try {
                parent = extended.resolve().asReferenceType();
            } catch (Exception e) {
                log.warn("父类 [{}] 无法解析，父类字段将被忽略", extended.getNameAsString());
                return;
            }
            if ("java.lang.Object".equals(parent.getQualifiedName())) {
                return;
            }
            resolveAstDeclaration(parent)
                    .ifPresentOrElse(
                            parentDeclaration -> addAstFields(parentDeclaration, metadata, seen),
                            () -> addResolvedFields(parent, metadata, seen));
        });
    }

    /**
     * 父类本身也是项目源码时，返回其 AST 声明（注释才能一并提取）；
     * 父类来自 jar / class 文件时返回 empty，退化为纯符号解析。
     */
    private Optional<ClassOrInterfaceDeclaration> resolveAstDeclaration(ResolvedReferenceType parent) {
        try {
            ResolvedReferenceTypeDeclaration typeDeclaration = parent.getTypeDeclaration().orElseThrow();
            if (typeDeclaration instanceof JavaParserClassDeclaration parserDeclaration) {
                return Optional.of(parserDeclaration.getWrappedNode());
            }
        } catch (Exception e) {
            log.debug("无法获取父类 AST 节点: {}", parent.describe(), e);
        }
        return Optional.empty();
    }

    /**
     * 父类来自二进制依赖时的兜底：用符号声明中的字段（无注释），全部祖先字段去重。
     */
    private void addResolvedFields(ResolvedReferenceType parent,
                                   ClassMetadata metadata, Set<String> seen) {
        try {
            ResolvedReferenceTypeDeclaration typeDeclaration = parent.getTypeDeclaration().orElseThrow();
            for (ResolvedFieldDeclaration field : typeDeclaration.getAllFields()) {
                if (field.isStatic() || !seen.add(field.getName())) {
                    continue;
                }
                ClassMetadata.FieldInfo fieldInfo = new ClassMetadata.FieldInfo();
                fieldInfo.setName(field.getName());
                ResolvedType fieldType = field.getType();
                fieldInfo.setFullType(fieldType.describe());
                fieldInfo.setType(TypeMapper.map(fieldType));
                fieldInfo.setColumn(isMappedColumn(fieldType));
                metadata.getFields().add(fieldInfo);
            }
        } catch (Exception e) {
            log.warn("解析父类 [{}] 字段失败，父类字段将被忽略", parent.getQualifiedName(), e);
        }
    }

    private static TypeName lenientClassName(String writtenType) {
        try {
            return ClassName.bestGuess(writtenType.replaceAll("<.*>", ""));
        } catch (Exception e) {
            return TypeName.OBJECT;
        }
    }

    /**
     * 判断字段是否为 MyBatis-Flex APT 会生成 TableDef 列的字段：
     * {@code Collection} / {@code Map} 及其子类型不是列，也不能作为等值查询条件。
     * 祖先求解失败时保持「是列」假设——若 APT 实际未生成该列，编译期会快速暴露。
     */
    static boolean isMappedColumn(ResolvedType type) {
        if (!type.isReference()) {
            return true;
        }
        ResolvedReferenceType reference = type.asReferenceType();
        if (isContainer(reference.getQualifiedName())) {
            return false;
        }
        try {
            for (ResolvedReferenceType ancestor : reference.getAllAncestors()) {
                if (isContainer(ancestor.getQualifiedName())) {
                    return false;
                }
            }
        } catch (Exception e) {
            log.debug("无法求解字段类型祖先，按映射列处理: {}", type.describe(), e);
        }
        return true;
    }

    private static boolean isContainer(String qualifiedName) {
        return "java.util.Collection".equals(qualifiedName) || "java.util.Map".equals(qualifiedName);
    }

    /**
     * 类型无法求解时按源码写法兜底识别 JDK 容器类型；自定义容器此时识别不出，
     * 编译期仍会快速失败。
     */
    private static boolean isContainerTypeName(String writtenType) {
        String raw = writtenType.replaceAll("<.*>", "").trim();
        String simple = raw.substring(raw.lastIndexOf('.') + 1);
        return raw.startsWith("java.util.") && JDK_CONTAINER_SIMPLE_NAMES.contains(simple);
    }

    private static final Set<String> JDK_CONTAINER_SIMPLE_NAMES = Set.of(
            "Collection", "List", "ArrayList", "LinkedList",
            "Set", "HashSet", "LinkedHashSet", "TreeSet",
            "Queue", "Deque", "ArrayDeque",
            "Map", "HashMap", "LinkedHashMap", "TreeMap", "SortedMap", "NavigableMap");
}
