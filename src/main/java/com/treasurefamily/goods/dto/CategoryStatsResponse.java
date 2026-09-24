package com.treasurefamily.goods.dto;

import com.treasurefamily.goods.Category;
import java.math.BigDecimal;

/** 对应前端 categoryStats */
public record CategoryStatsResponse(
        Category category,
        long count,
        BigDecimal value) {
}
