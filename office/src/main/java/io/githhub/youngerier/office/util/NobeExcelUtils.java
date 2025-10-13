package io.githhub.youngerier.office.util;

import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.DateUtils;
import com.alibaba.excel.write.handler.WriteHandler;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Excel工具类
 *
 * @author fanqingwei
 * @github <a href="https://github.com/alibaba/easyexcel">...</a>
 * @since 2023-12-07
 */
@Slf4j
public final class NobeExcelUtils {

    private NobeExcelUtils() {
        throw new AssertionError();
    }

    /**
     * 读取excel的所有sheet数据
     *
     * @param excel excel文件
     * @return List<Object>
     */
    @NotEmpty
    @SneakyThrows
    public static <T> List<T> read(MultipartFile excel, Class<T> clazz) {
        return EasyExcelUtils.read(excel.getOriginalFilename(), excel.getInputStream(), clazz);
    }

    /**
     * 读取excel的指定sheet数据
     *
     * @param excel   excel文件
     * @param sheetNo sheet序号(从0开始)
     * @return List<Object>
     */
    @NotEmpty
    @SneakyThrows
    public static <T> List<T> read(MultipartFile excel, int sheetNo, Class<T> clazz) {
        return EasyExcelUtils.read(excel.getOriginalFilename(), excel.getInputStream(), sheetNo, 1, clazz);
    }

    /**
     * 读取并导入数据
     *
     * @param excel    excel文件
     * @param importer 导入逻辑类
     * @param <T>      泛型
     */
    @SneakyThrows
    public static <T> void save(MultipartFile excel, EasyExcelUtils.ExcelImporter<T> importer, Class<T> clazz) {
        EasyExcelUtils.save(excel.getOriginalFilename(), excel.getInputStream(), importer, clazz);
    }

    /**
     * 导出excel
     *
     * @param response 响应类
     * @param dataList 数据列表
     * @param clazz    class类
     * @param <T>      泛型
     */
    @SneakyThrows
    public static <T> void export(HttpServletResponse response, List<T> dataList, Class<T> clazz) {
        export(response, DateUtils.format(new Date(), DateUtils.DATE_FORMAT_14), "导出数据", dataList, clazz);
    }

    /**
     * 导出excel
     *
     * @param response  响应类
     * @param fileName  文件名
     * @param sheetName sheet名
     * @param dataList  数据列表
     * @param clazz     class类
     * @param <T>       泛型
     */
    @SneakyThrows
    public static <T> void export(HttpServletResponse response, String fileName, String sheetName, List<T> dataList, Class<T> clazz) {
        appendFileHeaders(response, fileName);
        EasyExcelUtils.export(response.getOutputStream(), DateUtils.format(new Date(), DateUtils.DATE_FORMAT_14), "导出数据", dataList, clazz);

    }

    /**
     * 导出excel
     *
     * @param response     响应类
     * @param fileName     文件名
     * @param sheetName    sheet名
     * @param dataList     数据列表
     * @param clazz        class类
     * @param writeHandler 自定义处理器
     * @param <T>          泛型
     */
    @SneakyThrows
    public static <T> void export(HttpServletResponse response, String fileName, String sheetName, List<T> dataList, WriteHandler writeHandler, Class<T> clazz) {
        appendFileHeaders(response, fileName);
        EasyExcelUtils.export(response.getOutputStream(), DateUtils.format(new Date(), DateUtils.DATE_FORMAT_14), "导出数据", dataList, clazz);
    }

    /**
     * 获取构建类
     *
     * @param excel        excel文件
     * @param readListener excel监听类
     * @return ExcelReaderBuilder
     */
    @Nullable
    @SneakyThrows
    public static <T> ExcelReaderBuilder getReaderBuilder(MultipartFile excel, ReadListener<T> readListener, Class<T> clazz) {
        return EasyExcelUtils.getReaderBuilder(excel.getOriginalFilename(), excel.getInputStream(), readListener, clazz);
    }

    public static void appendFileHeaders(HttpServletResponse response, String fileName) throws Exception {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String name = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        response.setHeader("Content-disposition", String.format("attachment;filename=%s.xlsx", name));
    }
}