package com.treasurefamily.sale.dto;

import java.math.BigDecimal;

public record SaleLineView(String name, Integer qty, BigDecimal price, BigDecimal cost) {
}
