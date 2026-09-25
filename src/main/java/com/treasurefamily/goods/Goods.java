package com.treasurefamily.goods;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "goods",
        indexes = {
            @Index(name = "idx_goods_owner_id", columnList = "owner_id"),
            @Index(name = "idx_goods_owner_barcode", columnList = "owner_id, barcode")
        })
public class Goods {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "owner_id", length = 36)
    private String ownerId;

    @Column(length = 13)
    private String barcode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(length = 100)
    private String supplier;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "purchase_place", length = 100)
    private String purchasePlace;

    @Column(nullable = false)
    private Integer threshold;

    @Column(nullable = false)
    private Long createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = System.currentTimeMillis();
        }
        if (supplier == null) {
            supplier = "";
        }
        if (cost == null) {
            cost = BigDecimal.ZERO;
        }
        if (purchasePlace == null) {
            purchasePlace = "";
        }
    }

    public boolean isLowStock() {
        return threshold != null && threshold > 0 && stock != null && stock <= threshold;
    }

    public boolean isOutOfStock() {
        return stock == null || stock <= 0;
    }

    public BigDecimal lineValue() {
        if (price == null || stock == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(stock));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getPurchasePlace() {
        return purchasePlace;
    }

    public void setPurchasePlace(String purchasePlace) {
        this.purchasePlace = purchasePlace;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
