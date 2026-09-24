package com.treasurefamily.goods;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

/** 与前端 CATEGORIES 保持一致 */
public enum Category {
    FOOD_BEVERAGE("食品饮料"),
    FRESH_PRODUCE("生鲜果蔬"),
    DAILY_GOODS("日用百货"),
    HOME_KITCHEN("家居厨房"),
    STATIONERY("文具办公"),
    OTHER("其他");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Category fromLabel(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("分类不能为空");
        }
        return Arrays.stream(values())
                .filter(item -> item.label.equals(value) || item.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知分类: " + value));
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(Category::getLabel).toList();
    }
}
