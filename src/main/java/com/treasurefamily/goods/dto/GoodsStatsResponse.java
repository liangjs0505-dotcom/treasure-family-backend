package com.treasurefamily.goods.dto;

import java.math.BigDecimal;

/** 对应前端 GoodsStats */
public record GoodsStatsResponse(
        long totalKinds,
        long totalStock,
        BigDecimal totalValue,
        long lowStockCount) {
}
