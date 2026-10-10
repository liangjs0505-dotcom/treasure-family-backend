package com.treasurefamily.sale;

import com.treasurefamily.common.ApiResponse;
import com.treasurefamily.sale.dto.CheckoutRequest;
import com.treasurefamily.sale.dto.CompareResponse;
import com.treasurefamily.sale.dto.RankBoardResponse;
import com.treasurefamily.sale.dto.SaleDetailResponse;
import com.treasurefamily.sale.dto.TodaySummaryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping("/checkout")
    public ApiResponse<TodaySummaryResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.ok("结账完成", saleService.checkout(request));
    }

    @GetMapping("/sales/today")
    public ApiResponse<TodaySummaryResponse> today(@RequestParam int offset) {
        return ApiResponse.ok(saleService.today(offset));
    }

    @GetMapping("/sales/compare")
    public ApiResponse<CompareResponse> compare(
            @RequestParam(defaultValue = "week") String range, @RequestParam int offset) {
        return ApiResponse.ok(saleService.compare(range, offset));
    }

    @GetMapping("/sales/ranks")
    public ApiResponse<RankBoardResponse> ranks(@RequestParam int offset) {
        return ApiResponse.ok(saleService.ranks(offset));
    }

    @GetMapping("/sales/details")
    public ApiResponse<SaleDetailResponse> details(
            @RequestParam String from, @RequestParam String to, @RequestParam int offset) {
        return ApiResponse.ok(saleService.details(from, to, offset));
    }
}
