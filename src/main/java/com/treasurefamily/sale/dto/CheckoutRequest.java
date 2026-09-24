package com.treasurefamily.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckoutRequest(@NotEmpty(message = "账单是空的") @Valid List<CheckoutLineRequest> lines) {
}
