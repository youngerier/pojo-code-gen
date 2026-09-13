package io.github.youngerier.generator.model;

import com.squareup.javapoet.ClassName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link PackageStructure} 与 {@link GeneratedType} 的单元测试。
 */
class PackageStructureTest {

    private final PackageStructure packages = new PackageStructure("com.abc", "User");

    @Test
    void generatedTypeResolvesPackageAndClassName() {
        assertEquals("com.abc.model.dto", GeneratedType.DTO.packageName("com.abc"));
        assertEquals("UserDTO", GeneratedType.DTO.className("User"));
    }

    @Test
    void queryReusesRequestPackage() {
        assertEquals("com.abc.model.request", GeneratedType.QUERY.packageName("com.abc"));
        assertEquals("UserQuery", GeneratedType.QUERY.className("User"));
        assertEquals(packages.request().packageName(), packages.query().packageName());
    }

    @Test
    void typeReturnsFullyQualifiedClassName() {
        assertEquals(ClassName.get("com.abc.model.dto", "UserDTO"), packages.dto());
        assertEquals(ClassName.get("com.abc.service", "UserService"), packages.service());
        assertEquals(ClassName.get("com.abc.service.impl", "UserServiceImpl"), packages.serviceImpl());
        assertEquals(ClassName.get("com.abc.dal.repository", "UserRepository"), packages.repository());
        assertEquals(ClassName.get("com.abc.dal.mapper", "UserMapper"), packages.mapper());
        assertEquals(ClassName.get("com.abc.model.request", "UserRequest"), packages.request());
        assertEquals(ClassName.get("com.abc.model.response", "UserResponse"), packages.response());
        assertEquals(ClassName.get("com.abc.convertor", "UserConvertor"), packages.convertor());
        assertEquals(ClassName.get("com.abc.controller", "UserController"), packages.controller());
    }
}
