package com.treasurefamily.goods;

import com.treasurefamily.auth.CurrentUser;
import com.treasurefamily.common.BusinessException;
import com.treasurefamily.goods.dto.BarcodeLookupResponse;
import com.treasurefamily.goods.dto.CategoryStatsResponse;
import com.treasurefamily.goods.dto.GoodsRequest;
import com.treasurefamily.goods.dto.GoodsResponse;
import com.treasurefamily.goods.dto.GoodsStatsResponse;
import com.treasurefamily.goods.dto.IssuedBarcodeResponse;
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
    private final BarcodeCatalogRepository barcodeCatalogRepository;

    public GoodsService(GoodsRepository goodsRepository, BarcodeCatalogRepository barcodeCatalogRepository) {
        this.goodsRepository = goodsRepository;
        this.barcodeCatalogRepository = barcodeCatalogRepository;
    }

    @Transactional(readOnly = true)
    public BarcodeLookupResponse lookup(String raw) {
        String code = BarcodeCodes.requireRetailOrLabel(raw);
        Goods owned = goodsRepository.findByOwnerIdAndBarcode(currentOwnerId(), code).orElse(null);
        if (owned != null) {
            return new BarcodeLookupResponse(code, BarcodeCodes.internal(code), false, null, GoodsMapper.toResponse(owned));
        }
        if (BarcodeCodes.internal(code)) {
            return new BarcodeLookupResponse(code, true, true, null, null);
        }
        String catalogName = barcodeCatalogRepository.findById(code).map(BarcodeCatalog::getName).orElse(null);
        return new BarcodeLookupResponse(code, false, false, catalogName, null);
    }

    @Transactional
    public IssuedBarcodeResponse issueInternal() {
        String ownerId = currentOwnerId();
        for (int i = 0; i < 8; i++) {
            String code = BarcodeCodes.nextInternal();
            if (!goodsRepository.existsByOwnerIdAndBarcode(ownerId, code)) {
                return new IssuedBarcodeResponse(code);
            }
        }
        throw new BusinessException("暂时发不出店内码，请再试一次");
    }

    @Transactional
    public void backfillMissingBarcodes() {
        for (Goods goods : goodsRepository.findAll()) {
            if (goods.getBarcode() != null && !goods.getBarcode().isBlank()) {
                continue;
            }
            String ownerId = goods.getOwnerId() == null ? "" : goods.getOwnerId();
            String code = uniqueInternal(ownerId);
            goods.setBarcode(code);
            goodsRepository.save(goods);
        }
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
        String code = BarcodeCodes.requireRetailOrLabel(request.barcode());
        String ownerId = currentOwnerId();
        if (goodsRepository.existsByOwnerIdAndBarcode(ownerId, code)) {
            throw new BusinessException("这家店已经有这个条码");
        }
        Goods goods = new Goods();
        goods.setOwnerId(ownerId);
        requireMoney(request);
        if (request.stock() == null || request.stock() <= 0) {
            throw new BusinessException("入库数量必须大于 0");
        }
        GoodsMapper.apply(goods, request);
        goods.setBarcode(code);
        rememberName(code, goods.getName());
        return GoodsMapper.toResponse(goodsRepository.save(goods));
    }

    @Transactional
    public GoodsResponse update(String id, GoodsRequest request) {
        Goods goods = requireGoods(id);
        String code = BarcodeCodes.requireRetailOrLabel(request.barcode());
        if (goods.getBarcode() != null && !goods.getBarcode().equals(code)) {
            throw new BusinessException("条码不能改");
        }
        if (goodsRepository.findByOwnerIdAndBarcode(currentOwnerId(), code)
                .filter(other -> !other.getId().equals(goods.getId()))
                .isPresent()) {
            throw new BusinessException("这家店已经有这个条码");
        }
        requireMoney(request);
        GoodsMapper.apply(goods, request);
        goods.setBarcode(code);
        rememberName(code, goods.getName());
        return GoodsMapper.toResponse(goodsRepository.save(goods));
    }

    @Transactional
    public void delete(String id) {
        if (!goodsRepository.existsByIdAndOwnerId(id, currentOwnerId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "货物不存在");
        }
        goodsRepository.deleteById(id);
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

    private String uniqueInternal(String ownerId) {
        for (int i = 0; i < 8; i++) {
            String code = BarcodeCodes.nextInternal();
            if (!goodsRepository.existsByOwnerIdAndBarcode(ownerId, code)) {
                return code;
            }
        }
        throw new BusinessException("暂时发不出店内码，请再试一次");
    }

    private void requireMoney(GoodsRequest request) {
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("售价必须大于 0");
        }
        if (request.cost() == null || request.cost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("进价必须大于 0");
        }
    }

    private void rememberName(String code, String name) {
        if (BarcodeCodes.internal(code) || barcodeCatalogRepository.existsById(code)) {
            return;
        }
        BarcodeCatalog catalog = new BarcodeCatalog();
        catalog.setBarcode(code);
        catalog.setName(name);
        catalog.setUpdatedAt(System.currentTimeMillis());
        barcodeCatalogRepository.save(catalog);
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
        String barcode = goods.getBarcode() == null ? "" : goods.getBarcode();
        return name.contains(needle) || supplier.contains(needle) || barcode.contains(needle);
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
