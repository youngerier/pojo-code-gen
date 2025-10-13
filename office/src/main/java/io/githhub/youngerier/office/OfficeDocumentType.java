package io.githhub.youngerier.office;

import io.github.youngerier.support.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 办公文档类型
 *
 **/
@AllArgsConstructor
@Getter
public enum OfficeDocumentType implements DescriptiveEnum {

    WORD("word 文档"),

    EXCEL("excel 文档"),

    PPT("ppt 文档"),

    PDF("pdf 文档");

    private final String desc;
}
