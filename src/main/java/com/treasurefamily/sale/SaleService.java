package com.treasurefamily.sale;

import com.treasurefamily.auth.CurrentUser;
import com.treasurefamily.common.BusinessException;
import com.treasurefamily.goods.Goods;
import com.treasurefamily.goods.GoodsRepository;
import com.treasurefamily.sale.dto.CheckoutLineRequest;
import com.treasurefamily.sale.dto.CheckoutRequest;
import com.treasurefamily.sale.dto.ComparePointResponse;
import com.treasurefamily.sale.dto.CompareResponse;
import com.treasurefamily.sale.dto.RankBoardResponse;
import com.treasurefamily.sale.dto.RankItemResponse;
import com.treasurefamily.sale.dto.SaleDetailResponse;
import com.treasurefamily.sale.dto.SaleLineView;
import com.treasurefamily.sale.dto.SaleOrderView;
import com.treasurefamily.sale.dto.TodaySummaryResponse;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final SaleRepository saleRepository;
    private final SaleLineRepository saleLineRepository;
    private final GoodsRepository goodsRepository;

    public SaleService(
            SaleRepository saleRepository,
            SaleLineRepository saleLineRepository,
            GoodsRepository goodsRepository) {
        this.saleRepository = saleRepository;
        this.saleLineRepository = saleLineRepository;
        this.goodsRepository = goodsRepository;
    }

    @Transactional
    public TodaySummaryResponse checkout(CheckoutRequest request) {
        String ownerId = CurrentUser.require().userId();
        Map<String, Integer> qtyByGoods = new LinkedHashMap<>();
        for (CheckoutLineRequest line : request.lines()) {
            qtyByGoods.merge(line.goodsId(), line.qty(), Integer::sum);
        }

        Sale sale = new Sale();
        sale.setOwnerId(ownerId);
        sale.setCreatedAt(System.currentTimeMillis());
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        int soldQty = 0;

        for (Map.Entry<String, Integer> entry : qtyByGoods.entrySet()) {
            Goods goods = goodsRepository
                    .findByIdAndOwnerId(entry.getKey(), ownerId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "货物不存在"));
            int qty = entry.getValue();
            int stock = goods.getStock() == null ? 0 : goods.getStock();
            if (qty > stock) {
                throw new BusinessException("「" + goods.getName() + "」库存只有 " + stock + goods.getUnit());
            }
            BigDecimal price = goods.getPrice() == null ? BigDecimal.ZERO : goods.getPrice();
            BigDecimal cost = goods.getCost() == null ? BigDecimal.ZERO : goods.getCost();
            BigDecimal lineRevenue = price.multiply(BigDecimal.valueOf(qty));
            BigDecimal lineProfit = price.subtract(cost).multiply(BigDecimal.valueOf(qty));
            goods.setStock(stock - qty);
            goodsRepository.save(goods);

            SaleLine saleLine = new SaleLine();
            saleLine.setSaleId(requireSaleId(sale));
            saleLine.setOwnerId(ownerId);
            saleLine.setGoodsId(goods.getId());
            saleLine.setName(goods.getName());
            saleLine.setQty(qty);
            saleLine.setPrice(price);
            saleLine.setCost(cost);
            saleLineRepository.save(saleLine);

            revenue = revenue.add(lineRevenue);
            profit = profit.add(lineProfit);
            soldQty += qty;
        }

        sale.setRevenue(revenue);
        sale.setProfit(profit);
        sale.setSoldQty(soldQty);
        sale.setPayMethod(request.payMethod().name());
        saleRepository.save(sale);
        return today();
    }

    @Transactional(readOnly = true)
    public TodaySummaryResponse today() {
        String ownerId = CurrentUser.require().userId();
        long start = LocalDate.now(SHANGHAI).atStartOfDay(SHANGHAI).toInstant().toEpochMilli();
        List<Sale> sales = saleRepository.findByOwnerIdAndCreatedAtGreaterThanEqual(ownerId, start);
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal cardRevenue = BigDecimal.ZERO;
        int soldQty = 0;
        for (Sale sale : sales) {
            BigDecimal saleRevenue = sale.getRevenue() == null ? BigDecimal.ZERO : sale.getRevenue();
            revenue = revenue.add(saleRevenue);
            profit = profit.add(sale.getProfit() == null ? BigDecimal.ZERO : sale.getProfit());
            soldQty += sale.getSoldQty() == null ? 0 : sale.getSoldQty();
            if (PayMethod.CASH.name().equals(sale.getPayMethod())) {
                cashRevenue = cashRevenue.add(saleRevenue);
            } else if (PayMethod.CARD.name().equals(sale.getPayMethod())) {
                cardRevenue = cardRevenue.add(saleRevenue);
            }
        }
        return new TodaySummaryResponse(revenue, profit, sales.size(), soldQty, cashRevenue, cardRevenue);
    }

    @Transactional(readOnly = true)
    public RankBoardResponse ranks() {
        String ownerId = CurrentUser.require().userId();
        LocalDate today = LocalDate.now(SHANGHAI);
        long monthStart = today.minusDays(30).atStartOfDay(SHANGHAI).toInstant().toEpochMilli();
        Map<String, Goods> goodsById = goodsRepository.findByOwnerId(ownerId).stream()
                .collect(Collectors.toMap(Goods::getId, goods -> goods, (left, right) -> left, LinkedHashMap::new));

        List<Sale> monthSales = saleRepository.findByOwnerIdAndCreatedAtGreaterThanEqual(ownerId, 0L);
        List<String> saleIds = monthSales.stream().map(Sale::getId).toList();
        List<SaleLine> lines = saleIds.isEmpty()
                ? List.of()
                : saleLineRepository.findByOwnerIdAndSaleIdIn(ownerId, saleIds);
        Map<String, Long> saleTime = monthSales.stream()
                .collect(Collectors.toMap(Sale::getId, sale -> sale.getCreatedAt() == null ? 0L : sale.getCreatedAt()));

        Map<String, int[]> hotQty = new LinkedHashMap<>();
        Map<String, BigDecimal> hotRevenue = new LinkedHashMap<>();
        Map<String, BigDecimal> hotProfit = new LinkedHashMap<>();
        Map<String, Long> latestSaleAt = new LinkedHashMap<>();
        Map<String, BigDecimal> soldPrice = new LinkedHashMap<>();
        Map<String, BigDecimal> soldCost = new LinkedHashMap<>();
        Map<String, int[]> coldQty = new LinkedHashMap<>();
        Map<String, BigDecimal> coldRevenue = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (SaleLine line : lines) {
            int qty = line.getQty() == null ? 0 : line.getQty();
            BigDecimal price = line.getPrice() == null ? BigDecimal.ZERO : line.getPrice();
            BigDecimal lineCost = line.getCost() == null ? BigDecimal.ZERO : line.getCost();
            BigDecimal lineProfit = price.subtract(lineCost).multiply(BigDecimal.valueOf(qty));
            names.putIfAbsent(line.getGoodsId(), line.getName());
            long createdAt = saleTime.getOrDefault(line.getSaleId(), 0L);
            hotQty.computeIfAbsent(line.getGoodsId(), key -> new int[1])[0] += qty;
            hotRevenue.merge(line.getGoodsId(), price.multiply(BigDecimal.valueOf(qty)), BigDecimal::add);
            hotProfit.merge(line.getGoodsId(), lineProfit, BigDecimal::add);
            if (createdAt >= latestSaleAt.getOrDefault(line.getGoodsId(), -1L)) {
                latestSaleAt.put(line.getGoodsId(), createdAt);
                soldPrice.put(line.getGoodsId(), price);
                soldCost.put(line.getGoodsId(), lineCost);
            }
            if (createdAt >= monthStart) {
                coldQty.computeIfAbsent(line.getGoodsId(), key -> new int[1])[0] += qty;
                coldRevenue.merge(line.getGoodsId(), price.multiply(BigDecimal.valueOf(qty)), BigDecimal::add);
            }
        }

        List<RankItemResponse> hot = hotQty.entrySet().stream()
                .filter(entry -> entry.getValue()[0] > 0)
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> entry) -> entry.getValue()[0]).reversed())
                .limit(50)
                .map(entry -> rankItem(
                        entry.getKey(), goodsById, names, entry.getValue()[0], hotRevenue, hotProfit, soldPrice, soldCost, "", ""))
                .toList();

        List<RankItemResponse> cold = new ArrayList<>();
        for (Goods goods : goodsById.values()) {
            if (goods.getCreatedAt() == null || goods.getCreatedAt() > monthStart) {
                continue;
            }
            int sold = coldQty.getOrDefault(goods.getId(), new int[1])[0];
            String level = coldLevel(sold);
            if (level.isEmpty()) {
                continue;
            }
            cold.add(rankItem(
                    goods.getId(), goodsById, names, sold, coldRevenue, Map.of(), Map.of(), Map.of(), level, coldLabel(level)));
        }
        cold.sort(Comparator.comparingInt((RankItemResponse item) -> coldOrder(item.level()))
                .thenComparingInt(RankItemResponse::soldQty)
                .thenComparing(RankItemResponse::name));
        return new RankBoardResponse(hot, cold);
    }

    @Transactional(readOnly = true)
    public CompareResponse compare(String range) {
        String ownerId = CurrentUser.require().userId();
        LocalDate today = LocalDate.now(SHANGHAI);
        String raw = range == null ? "week" : range.trim().toLowerCase();
        String normalized = switch (raw) {
            case "month", "year" -> raw;
            default -> "week";
        };
        List<Bucket> buckets = switch (normalized) {
            case "month" -> monthBuckets(today);
            case "year" -> yearBuckets(today);
            default -> weekBuckets(today);
        };
        String unit = switch (normalized) {
            case "month" -> "week";
            case "year" -> "month";
            default -> "day";
        };
        LocalDate from = buckets.get(0).start();
        long start = from.atStartOfDay(SHANGHAI).toInstant().toEpochMilli();
        long end = today.plusDays(1).atStartOfDay(SHANGHAI).toInstant().toEpochMilli();
        List<Sale> sales = saleRepository.findByOwnerIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(ownerId, start, end);

        BigDecimal[] revenue = new BigDecimal[buckets.size()];
        BigDecimal[] profit = new BigDecimal[buckets.size()];
        int[] soldQty = new int[buckets.size()];
        for (int i = 0; i < buckets.size(); i++) {
            revenue[i] = BigDecimal.ZERO;
            profit[i] = BigDecimal.ZERO;
        }
        for (Sale sale : sales) {
            LocalDate day = java.time.Instant.ofEpochMilli(sale.getCreatedAt()).atZone(SHANGHAI).toLocalDate();
            int index = bucketIndex(buckets, day);
            if (index < 0) {
                continue;
            }
            revenue[index] = revenue[index].add(sale.getRevenue() == null ? BigDecimal.ZERO : sale.getRevenue());
            profit[index] = profit[index].add(sale.getProfit() == null ? BigDecimal.ZERO : sale.getProfit());
            soldQty[index] += sale.getSoldQty() == null ? 0 : sale.getSoldQty();
        }

        List<ComparePointResponse> points = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int totalQty = 0;
        for (int i = 0; i < buckets.size(); i++) {
            points.add(new ComparePointResponse(buckets.get(i).label(), revenue[i], profit[i], soldQty[i]));
            totalRevenue = totalRevenue.add(revenue[i]);
            totalProfit = totalProfit.add(profit[i]);
            totalQty += soldQty[i];
        }
        return new CompareResponse(normalized, unit, totalRevenue, totalProfit, totalQty, points);
    }

    @Transactional(readOnly = true)
    public SaleDetailResponse details(String fromText, String toText) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromText);
            to = LocalDate.parse(toText);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("日期不正确");
        }
        if (to.isBefore(from)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        if (to.isAfter(from.plusMonths(1))) {
            throw new BusinessException("最多选择一个月");
        }
        String ownerId = CurrentUser.require().userId();
        long start = from.atStartOfDay(SHANGHAI).toInstant().toEpochMilli();
        long end = to.plusDays(1).atStartOfDay(SHANGHAI).toInstant().toEpochMilli();
        List<Sale> sales = saleRepository.findByOwnerIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(ownerId, start, end);
        sales.sort(Comparator.comparing(Sale::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        List<String> saleIds = sales.stream().map(Sale::getId).toList();
        Map<String, List<SaleLine>> linesBySale = saleIds.isEmpty()
                ? Map.of()
                : saleLineRepository.findByOwnerIdAndSaleIdIn(ownerId, saleIds).stream()
                        .collect(Collectors.groupingBy(SaleLine::getSaleId));

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal cardRevenue = BigDecimal.ZERO;
        int soldQty = 0;
        List<SaleOrderView> orders = new ArrayList<>();
        for (Sale sale : sales) {
            BigDecimal saleRevenue = sale.getRevenue() == null ? BigDecimal.ZERO : sale.getRevenue();
            BigDecimal saleProfit = sale.getProfit() == null ? BigDecimal.ZERO : sale.getProfit();
            int qty = sale.getSoldQty() == null ? 0 : sale.getSoldQty();
            revenue = revenue.add(saleRevenue);
            profit = profit.add(saleProfit);
            soldQty += qty;
            if (PayMethod.CASH.name().equals(sale.getPayMethod())) {
                cashRevenue = cashRevenue.add(saleRevenue);
            } else if (PayMethod.CARD.name().equals(sale.getPayMethod())) {
                cardRevenue = cardRevenue.add(saleRevenue);
            }
            List<SaleLineView> lines = linesBySale.getOrDefault(sale.getId(), List.of()).stream()
                    .map(line -> new SaleLineView(
                            line.getName(),
                            line.getQty(),
                            line.getPrice() == null ? BigDecimal.ZERO : line.getPrice(),
                            line.getCost() == null ? BigDecimal.ZERO : line.getCost()))
                    .toList();
            orders.add(new SaleOrderView(
                    sale.getId(), sale.getCreatedAt(), saleRevenue, saleProfit, qty, sale.getPayMethod(), lines));
        }
        return new SaleDetailResponse(
                from.toString(), to.toString(), revenue, profit, soldQty, cashRevenue, cardRevenue, orders);
    }

    private List<Bucket> weekBuckets(LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Bucket> buckets = new ArrayList<>();
        for (LocalDate day = monday; !day.isAfter(today); day = day.plusDays(1)) {
            String label = switch (day.getDayOfWeek()) {
                case MONDAY -> "周一";
                case TUESDAY -> "周二";
                case WEDNESDAY -> "周三";
                case THURSDAY -> "周四";
                case FRIDAY -> "周五";
                case SATURDAY -> "周六";
                case SUNDAY -> "周日";
            };
            buckets.add(new Bucket(day, day, label));
        }
        return buckets;
    }

    private List<Bucket> monthBuckets(LocalDate today) {
        LocalDate first = today.withDayOfMonth(1);
        int length = first.lengthOfMonth();
        List<Bucket> buckets = new ArrayList<>();
        int week = 1;
        for (int startDay = 1; startDay <= today.getDayOfMonth(); startDay += 7) {
            int endDay = Math.min(startDay + 6, length);
            LocalDate start = first.withDayOfMonth(startDay);
            LocalDate end = first.withDayOfMonth(Math.min(endDay, today.getDayOfMonth()));
            buckets.add(new Bucket(start, end, "第" + week + "周"));
            week++;
        }
        return buckets;
    }

    private List<Bucket> yearBuckets(LocalDate today) {
        List<Bucket> buckets = new ArrayList<>();
        for (int month = 1; month <= today.getMonthValue(); month++) {
            LocalDate start = today.withMonth(month).withDayOfMonth(1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            if (end.isAfter(today)) {
                end = today;
            }
            buckets.add(new Bucket(start, end, month + "月"));
        }
        return buckets;
    }

    private int bucketIndex(List<Bucket> buckets, LocalDate day) {
        for (int i = 0; i < buckets.size(); i++) {
            Bucket bucket = buckets.get(i);
            if (!day.isBefore(bucket.start()) && !day.isAfter(bucket.end())) {
                return i;
            }
        }
        return -1;
    }

    private record Bucket(LocalDate start, LocalDate end, String label) {
    }

    private RankItemResponse rankItem(
            String goodsId,
            Map<String, Goods> goodsById,
            Map<String, String> names,
            int soldQty,
            Map<String, BigDecimal> revenueByGoods,
            Map<String, BigDecimal> profitByGoods,
            Map<String, BigDecimal> soldPrice,
            Map<String, BigDecimal> soldCost,
            String level,
            String levelLabel) {
        Goods goods = goodsById.get(goodsId);
        String name = goods != null ? goods.getName() : names.getOrDefault(goodsId, "已删除商品");
        String category = goods != null && goods.getCategory() != null ? goods.getCategory().getLabel() : "";
        String unit = goods != null && goods.getUnit() != null ? goods.getUnit() : "";
        String supplier = goods != null && goods.getSupplier() != null ? goods.getSupplier() : "";
        String place = goods != null && goods.getPurchasePlace() != null ? goods.getPurchasePlace() : "";
        BigDecimal price = soldPrice.getOrDefault(
                goodsId, goods != null && goods.getPrice() != null ? goods.getPrice() : BigDecimal.ZERO);
        BigDecimal cost = soldCost.getOrDefault(
                goodsId, goods != null && goods.getCost() != null ? goods.getCost() : BigDecimal.ZERO);
        int stock = goods != null && goods.getStock() != null ? goods.getStock() : 0;
        BigDecimal revenue = revenueByGoods.getOrDefault(goodsId, BigDecimal.ZERO);
        BigDecimal profit = profitByGoods.getOrDefault(goodsId, BigDecimal.ZERO);
        return new RankItemResponse(
                goodsId, name, category, unit, supplier, place, price, cost, stock, soldQty, revenue, profit, level, levelLabel);
    }

    private String coldLevel(int soldQty) {
        if (soldQty <= 0) {
            return "none";
        }
        if (soldQty <= 3) {
            return "few";
        }
        if (soldQty <= 9) {
            return "slow";
        }
        return "";
    }

    private String coldLabel(String level) {
        return switch (level) {
            case "none" -> "一个月没卖出";
            case "few" -> "卖得很少";
            case "slow" -> "动销偏慢";
            default -> "";
        };
    }

    private int coldOrder(String level) {
        return switch (level) {
            case "none" -> 0;
            case "few" -> 1;
            case "slow" -> 2;
            default -> 3;
        };
    }

    private String requireSaleId(Sale sale) {
        if (sale.getId() == null || sale.getId().isBlank()) {
            sale.setId(java.util.UUID.randomUUID().toString());
        }
        return sale.getId();
    }
}
