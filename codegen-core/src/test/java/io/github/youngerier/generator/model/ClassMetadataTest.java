package io.github.youngerier.generator.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassMetadataTest {

    @Test
    void testFieldInfoWithTypeName() {
        ClassMetadata.FieldInfo fieldInfo = new ClassMetadata.FieldInfo();
        
        // Test setting and getting TypeName
        TypeName stringType = ClassName.get(String.class);
        fieldInfo.setType(stringType);
        assertEquals(stringType, fieldInfo.getType());
        
        // Test backward compatibility method
        assertEquals("java.lang.String", fieldInfo.getTypeString());
        
        // Test other fields
        fieldInfo.setName("testField");
        fieldInfo.setFullType("java.lang.String");
        fieldInfo.setComment("Test field comment");
        fieldInfo.setPrimaryKey(true);
        
        assertEquals("testField", fieldInfo.getName());
        assertEquals("java.lang.String", fieldInfo.getFullType());
        assertEquals("Test field comment", fieldInfo.getComment());
        assertTrue(fieldInfo.isPrimaryKey());
    }
    
    @Test
    void testClassMetadata() {
        ClassMetadata classMetadata = new ClassMetadata();
        
        classMetadata.setPackageName("com.example");
        classMetadata.setClassName("TestEntity");
        classMetadata.setClassComment("Test entity class");
        
        assertEquals("com.example", classMetadata.getPackageName());
        assertEquals("TestEntity", classMetadata.getClassName());
        assertEquals("testEntity", classMetadata.getCamelClassName());
        assertEquals("Test entity class", classMetadata.getClassComment());
        assertNotNull(classMetadata.getFields());
        assertTrue(classMetadata.getFields().isEmpty());
    }
}