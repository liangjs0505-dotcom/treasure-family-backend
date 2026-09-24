package com.treasurefamily.goods;

import com.treasurefamily.common.ApiResponse;
import com.treasurefamily.goods.dto.CategoryStatsResponse;
import com.treasurefamily.goods.dto.GoodsResponse;
import com.treasurefamily.goods.dto.GoodsStatsResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final GoodsService goodsService;

    public StatsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("Treasure Family Backend");
    }

    @GetMapping("/stats")
    public ApiResponse<GoodsStatsResponse> stats() {
        return ApiResponse.ok(goodsService.stats());
    }

    @GetMapping("/stats/categories")
    public ApiResponse<List<CategoryStatsResponse>> categoryStats() {
        return ApiResponse.ok(goodsService.categoryStats());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<GoodsResponse>> alerts() {
        return ApiResponse.ok(goodsService.alerts());
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(Category.labels());
    }
}
