package com.treasurefamily.sale.dto;

import com.treasurefamily.sale.PayMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CheckoutRequest(
        @NotNull(message = "请选择结账方式") PayMethod payMethod,
        @NotEmpty(message = "账单是空的") @Valid List<CheckoutLineRequest> lines,
        @NotNull(message = "时区不正确") Integer offset) {
}
