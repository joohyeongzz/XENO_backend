package com.daewon.xeno_backend.utils;

import java.util.HashMap;
import java.util.Map;

public class CategoryUtils {

    private static final Map<String, String> categoryMap = new HashMap<>();
    private static final Map<String, String> categorySubMap = new HashMap<>();

    static {
        categoryMap.put("001", "상의");
        categoryMap.put("002", "하의");
        categoryMap.put("003", "아우터");
        categoryMap.put("004", "액세서리");
    }

    static {
        categorySubMap.put("001", "반팔");
        categorySubMap.put("002", "긴팔");
        categorySubMap.put("003", "청바지");
        categorySubMap.put("004", "반바지");
        categorySubMap.put("005", "면바지");
        categorySubMap.put("006", "나일론");
        categorySubMap.put("007", "후드집업");
        categorySubMap.put("008", "코트");
        categorySubMap.put("009", "바람막이");
        categorySubMap.put("010", "패딩");
        categorySubMap.put("011", "자켓");
        categorySubMap.put("012", "모자");
        categorySubMap.put("013", "안경");
        categorySubMap.put("014", "가방");

    }

    public static String getCategoryFromCode(String code) {
        String category = categoryMap.get(code);
        if (category == null) {
            throw new IllegalArgumentException("Unknown category code: " + code);
        }
        return category;
    }

    public static String getCategorySubFromCode(String code) {
        String categorySub = categorySubMap.get(code);
        if (categorySub == null) {
            throw new IllegalArgumentException("Unknown category code: " + code);
        }
        return categorySub;
    }
}