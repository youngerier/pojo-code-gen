package io.githhub.youngerier.office.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.handler.WriteHandler;
import io.github.youngerier.support.AssertUtils;
import io.github.youngerier.support.exception.BaseException;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel工具类
 *
 * @author zzw
 * @since 2023-01-04
 */
@Slf4j
public final class EasyExcelUtils {

    private EasyExcelUtils() {
        throw new AssertionError();
    }

    /**
     * 报表生成InputStream 上传OSS
     *
     * @param sheetName 名称
     * @param dataList  源数据
     * @param clazz     报表类型
     * @param <T>       报表类
     * @return InputStream
     */
    @SneakyThrows
    public static <T> InputStream exportInputStream(String sheetName, List<T> dataList, Class<T> clazz) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream, clazz).sheet(sheetName).doWrite(dataList);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 读取excel的所有sheet数据
     *
     * @return List<Object>
     */
    @NotEmpty
    public static <T> List<T> read(String excelFilename, InputStream excelInputStream, Class<T> clazz) {
        DataListener<T> dataListener = new DataListener<>();
        ExcelReaderBuilder builder = getReaderBuilder(excelFilename, excelInputStream, dataListener, clazz);
        if (builder == null) {
            return Collections.emptyList();
        }
        builder.doReadAll();
        return dataListener.getRecords();
    }

    /**
     * 读取excel的指定sheet数据
     *
     * @param sheetNo sheet序号(从0开始)
     * @return List<Object>
     */
    @NotEmpty
    public static <T> List<T> read(String excelFilename, InputStream excelInputStream, int sheetNo, Class<T> clazz) {
        return read(excelFilename, excelInputStream, sheetNo, 1, clazz);
    }

    /**
     * 读取excel的指定sheet数据
     *
     * @param sheetNo       sheet序号(从0开始)
     * @param headRowNumber 表头行数
     * @return List<Object>
     */
    @NotEmpty
    public static <T> List<T> read(String excelFilename, InputStream excelInputStream, int sheetNo, int headRowNumber, Class<T> clazz) {
        DataListener<T> dataListener = new DataListener<>();
        ExcelReaderBuilder builder = getReaderBuilder(excelFilename, excelInputStream, dataListener, clazz);
        if (builder == null) {
            return Collections.emptyList();
        }
        builder.sheet(sheetNo).headRowNumber(headRowNumber).doRead();
        return dataListener.getRecords();
    }

    /**
     * 读取并导入数据
     *
     * @param importer 导入逻辑类
     * @param <T>      泛型
     */
    public static <T> void save(String excelFilename, InputStream excelInputStream, ExcelImporter<T> importer, Class<T> clazz) {
        ImportListener<T> importListener = new ImportListener<>(importer);
        ExcelReaderBuilder builder = getReaderBuilder(excelFilename, excelInputStream, importListener, clazz);
        if (builder != null) {
            builder.doReadAll();
        }
    }


    /**
     * 导出excel
     *
     * @param fileName  文件名
     * @param sheetName sheet名
     * @param dataList  数据列表
     * @param clazz     class类
     * @param <T>       泛型
     */
    @SneakyThrows
    public static <T> void export(OutputStream outputStream, String fileName, String sheetName, List<T> dataList, Class<T> clazz) {
        EasyExcel.write(outputStream, clazz).sheet(sheetName).doWrite(dataList);
    }

    /**
     * 导出excel
     *
     * @param fileName     文件名
     * @param sheetName    sheet名
     * @param dataList     数据列表
     * @param clazz        class类
     * @param writeHandler 自定义处理器
     * @param <T>          泛型
     */
    @SneakyThrows
    public static <T> void export(OutputStream outputStream, String fileName, String sheetName, List<T> dataList, WriteHandler writeHandler, Class<T> clazz) {
        EasyExcel.write(outputStream, clazz).registerWriteHandler(writeHandler).sheet(sheetName).doWrite(dataList);
    }

    /**
     * 获取构建类
     *
     * @param
     * @param readListener excel监听类
     * @return ExcelReaderBuilder
     */
    @Nullable
    public static <T> ExcelReaderBuilder getReaderBuilder(String excelFilename, InputStream excelInputStream, ReadListener<T> readListener, Class<T> clazz) {
        AssertUtils.hasText(excelFilename, "请上传文件!");
        if ((!StringUtils.endsWithIgnoreCase(excelFilename, ".xls") && !StringUtils.endsWithIgnoreCase(excelFilename, ".xlsx") && !StringUtils.endsWithIgnoreCase(excelFilename, ".csv"))) {
            throw new BaseException("请上传正确的 excel 文件!");
        }
        InputStream inputStream;
        try {
            inputStream = new BufferedInputStream(excelInputStream);
            return EasyExcel.read(inputStream, clazz, readListener);
        } catch (Exception exception) {
            log.error("读取 excel 文件失败", exception);
        }
        return null;
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class DataListener<T> extends AnalysisEventListener<T> {
        /**
         * 缓存的数据列表
         */
        private final List<T> records = new ArrayList<>();

        @Override
        public void invoke(T data, AnalysisContext analysisContext) {
            records.add(data);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {

        }
    }

    public interface ExcelImporter<T> {

        /**
         * 导入数据逻辑
         *
         * @param data 数据集合
         */
        void save(List<T> data);
    }

    /**
     * Excel监听器
     */
    @Data
    @RequiredArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class ImportListener<T> extends AnalysisEventListener<T> {

        /**
         * 默认每隔3000条存储数据库
         */
        private int batchCount = 3000;

        /**
         * 缓存的数据列表
         */
        private List<T> records = new ArrayList<>();

        /**
         * 数据导入类
         */
        private final ExcelImporter<T> importer;

        @Override
        public void invoke(T data, AnalysisContext analysisContext) {
            records.add(data);
            // 达到BATCH_COUNT，则调用importer方法入库，防止数据几万条数据在内存，容易OOM
            if (records.size() >= batchCount) {
                // 调用importer方法
                importer.save(records);
                // 存储完成清理list
                records.clear();
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
            // 调用importer方法
            importer.save(records);
            // 存储完成清理list
            records.clear();
        }
    }


}
