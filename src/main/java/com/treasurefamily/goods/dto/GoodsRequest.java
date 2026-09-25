package com.treasurefamily.goods.dto;

import com.treasurefamily.goods.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 对应前端 GoodsFormData */
public record GoodsRequest(
        @NotBlank(message = "不能为空") String barcode,
        @NotBlank(message = "不能为空") String name,
        @NotNull(message = "不能为空") Category category,
        @NotNull(message = "不能为空") @DecimalMin(value = "0.0", inclusive = false, message = "必须大于 0") BigDecimal price,
        @NotNull(message = "不能为空") @Min(value = 0, message = "不能小于 0") Integer stock,
        @NotBlank(message = "不能为空") String unit,
        String supplier,
        @NotNull(message = "不能为空") @DecimalMin(value = "0.0", inclusive = false, message = "必须大于 0") BigDecimal cost,
        String purchasePlace,
        @NotNull(message = "不能为空") @Min(value = 0, message = "不能小于 0") Integer threshold) {
}
