package com.treasurefamily.sale.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleDetailResponse(
        String from,
        String to,
        BigDecimal revenue,
        BigDecimal profit,
        int soldQty,
        List<SaleOrderView> orders) {
}