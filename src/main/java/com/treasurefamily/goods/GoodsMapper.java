package com.treasurefamily.goods;

import com.treasurefamily.goods.dto.GoodsRequest;
import com.treasurefamily.goods.dto.GoodsResponse;
import java.math.BigDecimal;

public final class GoodsMapper {

    private GoodsMapper() {
    }

    public static void apply(Goods goods, GoodsRequest request) {
        goods.setBarcode(request.barcode().trim());
        goods.setName(request.name().trim());
        goods.setCategory(request.category());
        goods.setPrice(request.price());
        goods.setStock(request.stock());
        goods.setUnit(request.unit().trim());
        goods.setSupplier(request.supplier() == null ? "" : request.supplier().trim());
        goods.setCost(request.cost() == null ? BigDecimal.ZERO : request.cost());
        goods.setPurchasePlace(request.purchasePlace() == null ? "" : request.purchasePlace().trim());
        goods.setThreshold(request.threshold());
    }

    public static GoodsResponse toResponse(Goods goods) {
        BigDecimal price = goods.getPrice() == null ? BigDecimal.ZERO : goods.getPrice();
        BigDecimal cost = goods.getCost() == null ? BigDecimal.ZERO : goods.getCost();
        return new GoodsResponse(
                goods.getId(),
                goods.getBarcode() == null ? "" : goods.getBarcode(),
                goods.getName(),
                goods.getCategory(),
                price,
                goods.getStock(),
                goods.getUnit(),
                goods.getSupplier() == null ? "" : goods.getSupplier(),
                cost,
                goods.getPurchasePlace() == null ? "" : goods.getPurchasePlace(),
                goods.getThreshold(),
                goods.getCreatedAt(),
                goods.isLowStock(),
                goods.isOutOfStock());
    }
}
