package abc.office;

import io.github.youngerier.support.office.ExcelDocumentWriter;
import io.github.youngerier.support.office.OfficeDocumentTask;
import io.github.youngerier.support.office.export.DefaultEasyExcelDocumentWriter;
import io.github.youngerier.support.office.export.ExportExcelTaskInfo;
import io.github.youngerier.support.office.export.SpringExpressionExportExcelTask;
import io.github.youngerier.support.office.metadata.ExcelCellDescriptor;
import io.github.youngerier.support.office.util.NobeExcelUtils;

import java.util.Arrays;
import java.util.List;

public class ExportExcel {
    public static void main(String[] args) {

//        List<ExcelCellDescriptor> heads = Arrays.asList(
//                ExcelCellDescriptor.of("卡序列号", VccDTO.Fields.vccSeqNo)
//        );
//        NobeExcelUtils.appendFileHeaders(response, "查询 Vcc 列表导出");
//
//        ExcelDocumentWriter writer = DefaultEasyExcelDocumentWriter.of(response.getOutputStream(), heads);
//
//        OfficeDocumentTask task = new SpringExpressionExportExcelTask(ExportExcelTaskInfo.of("查询 Vcc 列表导出", writer), (ExportExcelDataFetcher<VccDTO>) (page, size) -> {
//            query.setQueryPage(page);
//            query.setQuerySize(size);
//            return vccService.queryVccs(query).getRecords();
//        });
//        task.run();
    }
}
