package com.treasurefamily.goods.dto;

import com.treasurefamily.goods.Category;
import java.math.BigDecimal;

/** 对应前端 Goods，并附带库存状态 */
public record GoodsResponse(
        String id,
        String barcode,
        String name,
        Category category,
        BigDecimal price,
        Integer stock,
        String unit,
        String supplier,
        BigDecimal cost,
        String purchasePlace,
        Integer threshold,
        Long createdAt,
        boolean lowStock,
        boolean outOfStock) {
}
