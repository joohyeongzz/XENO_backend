package com.daewon.xeno_backend.service;


import com.daewon.xeno_backend.dto.product.ProductRegisterDTO;
import com.daewon.xeno_backend.dto.product.ProductSizeDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    public List<ProductRegisterDTO> parseExcelFile(MultipartFile excel) throws IOException {
        List<ProductRegisterDTO> productList = new ArrayList<>();

        try (InputStream fis = excel.getInputStream(); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                ProductRegisterDTO product = new ProductRegisterDTO();
                product.setProductNumber(row.getCell(0).getStringCellValue());
                product.setName(row.getCell(1).getStringCellValue());
                product.setCategory(row.getCell(2).getStringCellValue());
                product.setPrice((long)row.getCell(3).getNumericCellValue());
                product.setColors(row.getCell(4).getStringCellValue());

                long sStock = (long) row.getCell(5).getNumericCellValue(); // S재고
                long mStock = (long) row.getCell(6).getNumericCellValue(); // M재고
                long lStock = (long) row.getCell(7).getNumericCellValue(); // L재고
                long xlStock = (long) row.getCell(8).getNumericCellValue(); // XL재고

                List<ProductSizeDTO> sizeList = new ArrayList<>();
                if (sStock > 0) {
                    sizeList.add(new ProductSizeDTO("S", sStock));
                }
                if (mStock > 0) {
                    sizeList.add(new ProductSizeDTO("M", mStock));
                }
                if (lStock > 0) {
                    sizeList.add(new ProductSizeDTO("L", lStock));
                }
                if (xlStock > 0) {
                    sizeList.add(new ProductSizeDTO("XL", xlStock));
                }

                // Set the size list to the product
                product.setSize(sizeList);

                product.setUrl_1(row.getCell(9).getStringCellValue());
                Cell cell10 = row.getCell(10);
                Cell cell11 = row.getCell(11);
                Cell cell12 = row.getCell(12);
                Cell cell13 = row.getCell(13);
                Cell cell14 = row.getCell(14);

// 각 셀의 값을 안전하게 읽어오기
                String url2 = (cell10 != null) ? cell10.getStringCellValue() : null;
                String url3 = (cell11 != null) ? cell11.getStringCellValue() : null;
                String url4 = (cell12 != null) ? cell12.getStringCellValue() : null;
                String url5 = (cell13 != null) ? cell13.getStringCellValue() : null;
                String url6 = (cell14 != null) ? cell14.getStringCellValue() : null;

// 제품 객체에 값 설정
                product.setUrl_2(url2);
                product.setUrl_3(url3);
                product.setUrl_4(url4);
                product.setUrl_5(url5);
                product.setUrl_6(url6);
                product.setDetail_url_1(row.getCell(15).getStringCellValue());



                productList.add(product);
            }
        }

        return productList;
    }
}

