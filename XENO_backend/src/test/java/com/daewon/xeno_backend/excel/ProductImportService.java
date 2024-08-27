package com.daewon.xeno_backend.excel;

import com.daewon.xeno_backend.domain.Products;
import com.daewon.xeno_backend.domain.ProductsColor;
import com.daewon.xeno_backend.domain.ProductsColorSize;
import com.daewon.xeno_backend.domain.Size;
import com.daewon.xeno_backend.repository.ProductsColorRepository;
import com.daewon.xeno_backend.repository.ProductsColorSizeRepository;
import com.daewon.xeno_backend.repository.ProductsRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SpringBootTest
@Log4j2
@Service
public class ProductImportService {

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private ProductsColorRepository productColorRepository;

    @Autowired
    private ProductsColorSizeRepository productColorSizeRepository;

    @Transactional
    public void importProductsFromExcel(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Products product = createProductFromRow(row);
                Products savedProduct = productsRepository.save(product);

                List<ProductsColor> colors = createProductColorsFromRow(row, savedProduct);
                for (ProductsColor color : colors) {
                    ProductsColor savedColor = productColorRepository.save(color);
                    List<ProductsColorSize> sizes = createProductColorSizesFromRow(row, savedColor);
                    productColorSizeRepository.saveAll(sizes);
                }
            }
        }
    }

    private Products createProductFromRow(Row row) {
        return Products.builder()
                .brandName(getCellStringValue(row.getCell(0)))
                .name(getCellStringValue(row.getCell(1)))
                .category(getCellStringValue(row.getCell(2)))
                .categorySub(getCellStringValue(row.getCell(3)))
                .price((long) row.getCell(4).getNumericCellValue())
                .priceSale((long) row.getCell(5).getNumericCellValue())
                .isSale(row.getCell(6).getBooleanCellValue())
                .productNumber(getCellStringValue(row.getCell(7)))
                .season(getCellStringValue(row.getCell(8)))
                .build();
    }

    private List<ProductsColor> createProductColorsFromRow(Row row, Products product) {
        List<ProductsColor> colors = new ArrayList<>();
        String[] colorNames = getCellStringValue(row.getCell(9)).split(",");
        for (String colorName : colorNames) {
            colors.add(ProductsColor.builder()
                    .products(product)
                    .color(colorName.trim())
                    .build());
        }
        return colors;
    }

    private List<ProductsColorSize> createProductColorSizesFromRow(Row row, ProductsColor color) {
        List<ProductsColorSize> sizes = new ArrayList<>();
        String[] sizeNames = getCellStringValue(row.getCell(10)).split(",");
        for (String sizeName : sizeNames) {
            sizes.add(ProductsColorSize.builder()
                    .productsColor(color)
                    .size(Size.valueOf(sizeName.trim()))
                    .build());
        }
        return sizes;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }

}
