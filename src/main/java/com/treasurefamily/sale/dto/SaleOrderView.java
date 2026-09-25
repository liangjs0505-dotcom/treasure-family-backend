package com.treasurefamily.sale.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleOrderView(
        String id,
        Long createdAt,
        BigDecimal revenue,
        BigDecimal profit,
        Integer soldQty,
        String payMethod,
        List<SaleLineView> lines) {
}
