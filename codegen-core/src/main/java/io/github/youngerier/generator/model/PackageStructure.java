package io.github.youngerier.generator.model;

import lombok.Getter;

/**
 * 包结构配置类，用于管理代码生成过程中各个组件的包路径和类名模板
 */
@Getter
public class PackageStructure {

    private final String basePackage;
    private final String dtoPackage;
    private final String servicePackage;
    private final String serviceImplPackage;
    private final String repositoryPackage;
    private final String requestPackage;
    private final String responsePackage;
    private final String convertorPackage;
    private final String controllerPackage;
    private final String mapperPackage;

    // 类名模板字段
    private final String dtoClassName;
    private final String serviceClassName;
    private final String serviceImplClassName;
    private final String repositoryClassName;
    private final String requestClassName;
    private final String responseClassName;
    private final String convertorClassName;
    private final String controllerClassName;
    private final String queryClassName;
    private final String mapperClassName;

    /**
     * 使用基础包名创建包结构配置，默认各组件包名和类名模板基于标准约定生成
     *
     * @param basePackage 基础包名
     */
    public PackageStructure(String basePackage, String entityName) {
        this.basePackage = basePackage;
        this.dtoPackage = basePackage + ".model.dto";
        this.servicePackage = basePackage + ".service";
        this.serviceImplPackage = basePackage + ".service.impl";
        this.repositoryPackage = basePackage + ".dal.repository";
        this.mapperPackage = basePackage + ".dal.mapper";
        this.requestPackage = basePackage + ".model.request";
        this.responsePackage = basePackage + ".model.response";
        this.convertorPackage = basePackage + ".convertor";
        this.controllerPackage = basePackage + ".controller";

        this.dtoClassName = entityName + "DTO";
        this.serviceClassName = entityName + "Service";
        this.serviceImplClassName = entityName + "ServiceImpl";
        this.repositoryClassName = entityName + "Repository";
        this.requestClassName = entityName + "Request";
        this.responseClassName = entityName + "Response";
        this.convertorClassName = entityName + "Convertor";
        this.controllerClassName = entityName + "Controller";
        this.queryClassName = entityName + "Query";
        this.mapperClassName = entityName + "Mapper";
    }

}