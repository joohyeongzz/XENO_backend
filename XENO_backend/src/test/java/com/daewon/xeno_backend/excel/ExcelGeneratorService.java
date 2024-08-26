package com.daewon.xeno_backend.excel;

import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;

@SpringBootTest
@Log4j2
@Service
public class ExcelGeneratorService {
    public void generateSampleExcelFile(String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] columns = {"브랜드 이름", "상품명", "카테고리", "서브 카테고리", "가격", "할인 가격", "할인여부", "제품번호", "계절", "색상", "사이즈"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            // 샘플 데이터 추가
            addSampleData(sheet);

            // 열 너비 자동 조정
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 파일 저장
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void addSampleData(Sheet sheet) {
        addProductRow(sheet, 1, "Nike", "Air Max", "Shoes", "Sneakers", 120000, 100000, true, "NKE001", "SS2023", "Black,White,Red", "250,260,270,280");
        addProductRow(sheet, 2, "Adidas", "Superstar", "Shoes", "Casual", 110000, 110000, false, "ADS001", "FW2023", "White,Black", "240,250,260,270");
        addProductRow(sheet, 3, "Gucci", "GG Marmont", "Bags", "Shoulder Bags", 2500000, 2250000, true, "GCI001", "FW2023", "Black,Red", "One Size");
        addProductRow(sheet, 4, "Zara", "Oversized Blazer", "Clothing", "Jackets", 89000, 89000, false, "ZRA001", "SS2024", "Beige,Black", "S,M,L,XL");
        addProductRow(sheet, 5, "H&M", "Slim Fit Jeans", "Clothing", "Pants", 39900, 29900, true, "HNM001", "SS2023", "Blue,Black", "28,30,32,34");
    }

    private void addProductRow(Sheet sheet, int rowNum, String brandName, String name, String category, String categorySub,
                               long price, long priceSale, boolean isSale, String productNumber, String season,
                               String colors, String sizes) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(brandName);
        row.createCell(1).setCellValue(name);
        row.createCell(2).setCellValue(category);
        row.createCell(3).setCellValue(categorySub);
        row.createCell(4).setCellValue(price);
        row.createCell(5).setCellValue(priceSale);
        row.createCell(6).setCellValue(isSale);
        row.createCell(7).setCellValue(productNumber);
        row.createCell(8).setCellValue(season);
        row.createCell(9).setCellValue(colors);
        row.createCell(10).setCellValue(sizes);
    }
}
