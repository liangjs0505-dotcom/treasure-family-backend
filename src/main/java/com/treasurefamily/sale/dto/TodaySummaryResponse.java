package com.treasurefamily.sale.dto;

import java.math.BigDecimal;

public record TodaySummaryResponse(BigDecimal revenue, BigDecimal profit, int orderCount, int soldQty) {
}
