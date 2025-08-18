
package com.example.generator.model;

import lombok.Getter;

@Getter
public class PackageLayout {

    private final String basePackage;
    private final String dtoPackage;
    private final String servicePackage;
    private final String serviceImplPackage;
    private final String repositoryPackage;
    private final String requestPackage;
    private final String responsePackage;
    private final String convertorPackage;

    public PackageLayout(String basePackage) {
        this.basePackage = basePackage;
        this.dtoPackage = basePackage + ".model.dto";
        this.servicePackage = basePackage + ".service";
        this.serviceImplPackage = basePackage + ".service.impl";
        this.repositoryPackage = basePackage + ".repository";
        this.requestPackage = basePackage + ".model.request";
        this.responsePackage = basePackage + ".model.response";
        this.convertorPackage = basePackage + ".convertor";
    }

    public String getDtoClassName(String entityName) {
        return entityName + "DTO";
    }

    public String getRequestClassName(String entityName) {
        return entityName + "Request";
    }

    public String getResponseClassName(String entityName) {
        return entityName + "Response";
    }

    public String getConvertorClassName(String entityName) {
        return entityName + "Convertor";
    }

    public String getQueryClassName(String entityName) {
        return entityName + "Query";
    }

    public String getRepositoryClassName(String entityName) {
        return entityName + "Repository";
    }

    public String getServiceClassName(String entityName) {
        return entityName + "Service";
    }

    public String getServiceImplClassName(String entityName) {
        return entityName + "ServiceImpl";
    }
}
