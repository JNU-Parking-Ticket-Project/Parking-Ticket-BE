package com.jnu.ticketapi.excel;

import com.jnu.ticketapi.api.council.model.response.ExcelDataTransfer;
import com.jnu.ticketapi.api.council.service.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class CreateExcelTest {

    @DisplayName("엑셀파일 생성 및 저장 테스트")
    @Test
    public void createExcel() throws Exception {
        List<ExcelDataTransfer> data = new ArrayList<>();
        ExcelDataTransfer r1 = ExcelDataTransfer.builder()
                .carIsLight(String.valueOf(true))
                .userAffiliation("affiliation")
                .userEmail("email")
                .userName("name")
                .userPhoneNumber("phonenum")
                .userSector("sector")
                .carNumber("carnum")
                .studentNumber("123456")
                .build();
        ExcelDataTransfer r2 = ExcelDataTransfer.builder()
                .carIsLight(String.valueOf(true))
                .userAffiliation("affiliation2")
                .userEmail("email2")
                .userName("name2")
                .userPhoneNumber("phonenum2")
                .userSector("sector2")
                .carNumber("carnum2")
                .studentNumber("123456")
                .build();
        data.add(r1);
        data.add(r2);

        Workbook workbook = ExcelUtil.makeExcel(data);
    }
}

