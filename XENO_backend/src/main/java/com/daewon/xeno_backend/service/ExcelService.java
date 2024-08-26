package com.daewon.xeno_backend.service;


import com.daewon.xeno_backend.dto.product.ProductRegisterDTO;
import com.daewon.xeno_backend.dto.product.ProductSizeDTO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    public List<ProductRegisterDTO> parseExcelFile(String filePath) throws IOException {
        List<ProductRegisterDTO> productList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
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

                productList.add(product);
            }
        }

        return productList;
    }
}
