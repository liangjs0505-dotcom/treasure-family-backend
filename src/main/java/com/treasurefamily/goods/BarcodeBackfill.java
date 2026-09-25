package com.treasurefamily.goods;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 旧库存没有条码时补一张店内码，保证每件货都有码。 */
@Component
public class BarcodeBackfill implements ApplicationRunner {

    private final GoodsService goodsService;

    public BarcodeBackfill(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        goodsService.backfillMissingBarcodes();
    }
}
