package com.treasurefamily.sale.dto;

import java.math.BigDecimal;

public record RankItemResponse(
        String goodsId,
        String name,
        String category,
        String unit,
        String supplier,
        String purchasePlace,
        BigDecimal price,
        BigDecimal cost,
        int stock,
        int soldQty,
        BigDecimal revenue,
        BigDecimal profit,
        String level,
        String levelLabel) {
}
