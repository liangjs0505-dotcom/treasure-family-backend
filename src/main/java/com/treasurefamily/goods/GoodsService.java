package com.treasurefamily.goods;

import com.treasurefamily.auth.CurrentUser;
import com.treasurefamily.common.BusinessException;
import com.treasurefamily.goods.dto.CategoryStatsResponse;
import com.treasurefamily.goods.dto.GoodsRequest;
import com.treasurefamily.goods.dto.GoodsResponse;
import com.treasurefamily.goods.dto.GoodsStatsResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodsService {

    private final GoodsRepository goodsRepository;

    public GoodsService(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    @Transactional(readOnly = true)
    public List<GoodsResponse> list(String keyword, String category, Boolean onlyLow, String sortBy) {
        return ownedGoods().stream()
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> matchesCategory(item, category))
                .filter(item -> onlyLow == null || !onlyLow || item.isLowStock())
                .sorted(sortComparator(sortBy).reversed())
                .map(GoodsMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GoodsResponse getById(String id) {
        return GoodsMapper.toResponse(requireGoods(id));
    }

    @Transactional
    public GoodsResponse create(GoodsRequest request) {
        Goods goods = new Goods();
        goods.setOwnerId(currentOwnerId());
        GoodsMapper.apply(goods, request);
        return GoodsMapper.toResponse(goodsRepository.save(goods));
    }

    @Transactional
    public GoodsResponse update(String id, GoodsRequest request) {
        Goods goods = requireGoods(id);
        GoodsMapper.apply(goods, request);
        return GoodsMapper.toResponse(goodsRepository.save(goods));
    }

    @Transactional
    public void delete(String id) {
        if (!goodsRepository.existsByIdAndOwnerId(id, currentOwnerId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "货物不存在");
        }
        goodsRepository.deleteById(id);
    }

    @Transactional
    public void clearAll() {
        goodsRepository.deleteByOwnerId(currentOwnerId());
    }

    @Transactional(readOnly = true)
    public GoodsStatsResponse stats() {
        List<Goods> goods = ownedGoods();
        long totalStock = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        long lowStockCount = 0;
        for (Goods item : goods) {
            totalStock += item.getStock() == null ? 0 : item.getStock();
            totalValue = totalValue.add(item.lineValue());
            if (item.isLowStock()) {
                lowStockCount++;
            }
        }
        return new GoodsStatsResponse(goods.size(), totalStock, totalValue, lowStockCount);
    }

    @Transactional(readOnly = true)
    public List<CategoryStatsResponse> categoryStats() {
        Map<Category, List<Goods>> grouped = ownedGoods().stream()
                .collect(Collectors.groupingBy(Goods::getCategory));
        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal value = entry.getValue().stream()
                            .map(Goods::lineValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CategoryStatsResponse(entry.getKey(), entry.getValue().size(), value);
                })
                .sorted(Comparator.comparing(CategoryStatsResponse::value).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GoodsResponse> alerts() {
        return ownedGoods().stream()
                .filter(Goods::isLowStock)
                .sorted(Comparator.comparing(Goods::getStock))
                .map(GoodsMapper::toResponse)
                .toList();
    }

    private List<Goods> ownedGoods() {
        return goodsRepository.findByOwnerId(currentOwnerId());
    }

    private String currentOwnerId() {
        return CurrentUser.require().userId();
    }

    private Goods requireGoods(String id) {
        return goodsRepository.findByIdAndOwnerId(id, currentOwnerId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "货物不存在"));
    }

    private boolean matchesKeyword(Goods goods, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        String name = goods.getName() == null ? "" : goods.getName().toLowerCase(Locale.ROOT);
        String supplier = goods.getSupplier() == null ? "" : goods.getSupplier().toLowerCase(Locale.ROOT);
        return name.contains(needle) || supplier.contains(needle);
    }

    private boolean matchesCategory(Goods goods, String category) {
        if (category == null || category.isBlank() || "全部".equals(category)) {
            return true;
        }
        return goods.getCategory() == Category.fromLabel(category);
    }

    private Comparator<Goods> sortComparator(String sortBy) {
        if ("price".equals(sortBy)) {
            return Comparator.comparing(Goods::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("stock".equals(sortBy)) {
            return Comparator.comparing(Goods::getStock, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return Comparator.comparing(Goods::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
