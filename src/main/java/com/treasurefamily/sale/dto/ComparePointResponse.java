package com.treasurefamily.sale.dto;

import java.math.BigDecimal;

public record ComparePointResponse(String label, BigDecimal revenue, BigDecimal profit, int soldQty) {
}
