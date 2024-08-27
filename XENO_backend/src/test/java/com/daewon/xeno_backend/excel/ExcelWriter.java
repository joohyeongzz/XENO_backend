package com.daewon.xeno_backend.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;

@SpringBootApplication
public class ExcelWriter {

    public static void main(String[] args) {
        SpringApplication.run(ExcelWriter.class, args);
    }
}

@Component
class ExcelWriter2 implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 시트 생성
            Sheet sheet = workbook.createSheet("ExampleSheet");

            // 시트의 첫번째 행 생성 (헤더 행)
            Row headerRow = sheet.createRow(0);

            // 헤더 행에 셀 생성 및 값 설정
            headerRow.createCell(0).setCellValue("이름");
            headerRow.createCell(1).setCellValue("나이");
            headerRow.createCell(2).setCellValue("이메일");

            // 엑셀 파일 저장
            try (FileOutputStream outputStream = new FileOutputStream("excel_file.xlsx")) {
                workbook.write(outputStream);
                System.out.println("엑셀 파일이 생성되었습니다.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}