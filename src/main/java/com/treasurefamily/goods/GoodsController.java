package com.treasurefamily.goods;

import com.treasurefamily.common.ApiResponse;
import com.treasurefamily.goods.dto.GoodsRequest;
import com.treasurefamily.goods.dto.GoodsResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping
    public ApiResponse<List<GoodsResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean onlyLow,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy) {
        return ApiResponse.ok(goodsService.list(keyword, category, onlyLow, sortBy));
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(goodsService.getById(id));
    }

    @PostMapping
    public ApiResponse<GoodsResponse> create(@Valid @RequestBody GoodsRequest request) {
        return ApiResponse.ok("添加成功", goodsService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<GoodsResponse> update(
            @PathVariable String id, @Valid @RequestBody GoodsRequest request) {
        return ApiResponse.ok("保存成功", goodsService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        goodsService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @DeleteMapping
    public ApiResponse<Void> clearAll() {
        goodsService.clearAll();
        return ApiResponse.ok("已清空", null);
    }
}
