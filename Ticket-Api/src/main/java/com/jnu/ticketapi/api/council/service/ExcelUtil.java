package com.jnu.ticketapi.api.council.service;

import com.jnu.ticketapi.api.council.model.response.ExcelDataTransfer;
import com.jnu.ticketcommon.consts.ExcelTemplate;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.List;

public class ExcelUtil {

    private static final String SHEET_NAME = "1차신청결과";

    @Deprecated(since = "아 이걸 찾다니 ㅋㅋ")
    public static Workbook makeExcel(List<ExcelDataTransfer> data) throws Exception{

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            CellStyle headStyle = workbook.createCellStyle();
            headStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_BLUE.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
            font.setFontHeightInPoints((short) 13);
            headStyle.setFont(font);

            int rowNo = 0;
            int cnt = ExcelTemplate.class.getDeclaredFields().length;
            Row headerRow = sheet.createRow(rowNo++);
            for(int i=0; i<cnt; i++){
                headerRow.createCell(i).setCellValue(String.valueOf(ExcelTemplate.class.getDeclaredFields()[i].get(new ExcelTemplate())));
                headerRow.getCell(i).setCellStyle(headStyle);
            }

            for(ExcelDataTransfer dto : data){
                Row row = sheet.createRow(rowNo++);
                Field[] fields = dto.getClass().getDeclaredFields();
                for (int i=0; i<cnt; i++) {
                    fields[i].setAccessible(true);
                    row.createCell(i).setCellValue(String.valueOf(fields[i].get(dto)));
                }
            }

            FileOutputStream fos = new FileOutputStream("/Users/park/test.xlsx"); // file 생성
            workbook.write(fos); // excel 저장
            return workbook;

        }

    }
}
