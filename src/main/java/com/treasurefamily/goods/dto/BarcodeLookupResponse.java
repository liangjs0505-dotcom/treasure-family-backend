package com.treasurefamily.goods.dto;

/** 扫码结果。blocked 为 true 时不要当成新商品录入。 */
public record BarcodeLookupResponse(
        String barcode,
        boolean internal,
        boolean blocked,
        String catalogName,
        GoodsResponse goods) {
}
