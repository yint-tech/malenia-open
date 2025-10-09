package cn.iinti.malenia2.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ExcelUtils {
    public static void setExcelExportResponseHeader(HttpServletResponse response, String fileName) {
        fileName = fileName.replaceAll("\\s", "_");
        // fileName = new String(fileName.getBytes(), Charsets.ISO_8859_1);
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");

    }

    private static final DateTimeFormatter exportDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static <T> void doExport(Class<T> type, BaseMapper<T> dao, QueryWrapper<T> wrapper, OutputStream os)
            throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(1024 * 8)) {
            Sheet sheet1 = wb.createSheet("sheet1");
            int excelRow = 0;
            Row titleRow = sheet1.createRow(excelRow++);
            titleRow.createCell(0).setCellValue("index");

            List<Field> columns = Lists.newArrayList(type.getDeclaredFields())
                    .stream().filter(
                            input -> !input.isSynthetic()
                                    && !Modifier.isStatic(input.getModifiers()))
                    .collect(Collectors.toList());

            for (int i = 0; i < columns.size(); i++) {
                Cell cell = titleRow.createCell(i + 1);
                Field field = columns.get(i);
                cell.setCellValue(field.getName());
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
            }

            long totalSize = dao.selectCount(wrapper);
            int pageSize = 1024;
            int cursor = 0;
            int count = 1;

            while (cursor < totalSize) {
                List<T> dataList = dao.selectList(wrapper.clone().last("limit " + cursor + "," + (cursor + pageSize)));
                cursor += pageSize;
                for (T t : dataList) {
                    Row dataRow = sheet1.createRow(excelRow++);
                    dataRow.createCell(0).setCellValue(count++);

                    for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                        setCellValue(columns.get(columnIndex), dataRow.createCell(columnIndex + 1), t);
                    }
                }
            }
            wb.write(os);
        }
    }

    private static <T> void setCellValue(Field field, Cell cell, T t) {
        if (field == null || cell == null || t == null) {
            return;
        }
        Object value;
        try {
            value = field.get(t);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (value == null) {
            return;
        }

        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof String) {
            cell.setCellValue(value.toString());
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Calendar) {
            cell.setCellValue((Calendar) value);
        } else if (value instanceof RichTextString) {
            cell.setCellValue((RichTextString) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(exportDateTimeFormatter));
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
