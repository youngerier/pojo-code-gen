package io.github.youngerier.generator.analysis;

import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.resolution.types.ResolvedWildcard;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import java.util.List;

/**
 * 将 JavaParser 的 {@link ResolvedType} 映射为 JavaPoet 的 {@link TypeName}。
 *
 * <p><b>为什么不能用 {@code ClassName.bestGuess(type.describe())}</b>：describe() 对
 * 泛型字段返回 {@code java.util.List<java.lang.String>} 这类带尖括号的字符串，bestGuess
 * 直接抛异常导致整个生成中断；且它无法表达通配符、数组、类型变量。这里递归处理全部形态。
 */
final class TypeMapper {

    private TypeMapper() {
    }

    static TypeName map(ResolvedType type) {
        if (type.isArray()) {
            return ArrayTypeName.of(map(type.asArrayType().getComponentType()));
        }
        if (type.isPrimitive()) {
            return mapPrimitive(type.asPrimitive());
        }
        if (type.isVoid()) {
            return TypeName.VOID;
        }
        if (type.isTypeVariable()) {
            return mapTypeVariable(type.asTypeVariable());
        }
        if (type.isWildcard()) {
            return mapWildcard(type.asWildcard());
        }
        if (type.isReference()) {
            return mapReference(type.asReferenceType());
        }
        return TypeName.OBJECT;
    }

    private static TypeName mapReference(ResolvedReferenceType reference) {
        ClassName raw = ClassName.bestGuess(reference.getQualifiedName());
        List<ResolvedType> typeArguments = safeTypeArguments(reference);
        if (typeArguments.isEmpty()) {
            return raw;
        }
        TypeName[] arguments = typeArguments.stream()
                .map(TypeMapper::map)
                .toArray(TypeName[]::new);
        return ParameterizedTypeName.get(raw, arguments);
    }

    private static TypeName mapWildcard(ResolvedWildcard wildcard) {
        if (!wildcard.isBounded()) {
            return WildcardTypeName.subtypeOf(TypeName.OBJECT);
        }
        TypeName bound = map(wildcard.getBoundedType());
        // 下边界（super）→ supertypeOf；上边界（extends）→ subtypeOf
        return wildcard.isLowerBounded()
                ? WildcardTypeName.supertypeOf(bound)
                : WildcardTypeName.subtypeOf(bound);
    }

    private static TypeName mapTypeVariable(ResolvedTypeVariable typeVariable) {
        return map(typeVariable.asTypeParameter().getUpperBound());
    }

    private static TypeName mapPrimitive(ResolvedPrimitiveType primitive) {
        return switch (primitive) {
            case BOOLEAN -> TypeName.BOOLEAN;
            case CHAR -> TypeName.CHAR;
            case BYTE -> TypeName.BYTE;
            case SHORT -> TypeName.SHORT;
            case INT -> TypeName.INT;
            case LONG -> TypeName.LONG;
            case FLOAT -> TypeName.FLOAT;
            case DOUBLE -> TypeName.DOUBLE;
        };
    }

    /**
     * 取引用类型的实际类型参数；原始类型（raw type）或求解不完整时返回空列表，不抛异常。
     */
    private static List<ResolvedType> safeTypeArguments(ResolvedReferenceType reference) {
        try {
            return reference.typeParametersValues();
        } catch (Exception e) {
            return List.of();
        }
    }
}
