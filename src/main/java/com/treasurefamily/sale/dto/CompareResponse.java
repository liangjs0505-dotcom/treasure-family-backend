package com.treasurefamily.sale.dto;

import java.math.BigDecimal;
import java.util.List;

public record CompareResponse(
        String range,
        String unit,
        BigDecimal revenue,
        BigDecimal profit,
        int soldQty,
        List<ComparePointResponse> points) {
}
