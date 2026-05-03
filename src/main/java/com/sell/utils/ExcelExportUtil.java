package com.sell.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExcelExportUtil {

    public static byte[] toXlsx(String sheetName, List<String> headers, List<Map<String, Object>> rows, List<String> keys) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            bold.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(bold);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(headerStyle);
            }
            int r = 1;
            for (Map<String, Object> row : rows) {
                Row datRow = sheet.createRow(r++);
                for (int i = 0; i < keys.size(); i++) {
                    Object v = row.get(keys.get(i));
                    Cell c = datRow.createCell(i);
                    if (v == null) {
                        c.setCellValue("");
                    } else if (v instanceof Number) {
                        c.setCellValue(((Number) v).doubleValue());
                    } else {
                        c.setCellValue(v.toString());
                    }
                }
            }
            for (int i = 0; i < headers.size(); i++) sheet.autoSizeColumn(i);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel 导出失败", e);
        }
    }
}
