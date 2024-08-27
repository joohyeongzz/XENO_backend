package com.daewon.xeno_backend.excel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private ExcelGeneratorService excelGeneratorService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            excelGeneratorService.generateSampleExcelFile("sample_products.xlsx");
            System.out.println("Sample Excel file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
