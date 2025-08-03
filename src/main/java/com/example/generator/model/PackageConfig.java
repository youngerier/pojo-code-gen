
package com.example.generator.model;

import lombok.Getter;

@Getter
public class PackageConfig {

    private final String basePackage;
    private final String dtoPackage;
    private final String servicePackage;
    private final String serviceImplPackage;
    private final String repositoryPackage;
    private final String requestPackage;
    private final String responsePackage;
    private final String convertorPackage;

    public PackageConfig(String basePackage) {
        this.basePackage = basePackage;
        this.dtoPackage = basePackage + ".model.dto";
        this.servicePackage = basePackage + ".service";
        this.serviceImplPackage = basePackage + ".service.impl";
        this.repositoryPackage = basePackage + ".repository";
        this.requestPackage = basePackage + ".model.request";
        this.responsePackage = basePackage + ".model.response";
        this.convertorPackage = basePackage + ".convertor";
    }
}
