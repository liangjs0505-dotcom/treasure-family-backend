package com.treasurefamily.goods;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 各店共用的条码名称。只有包装上的商品码会进来，不含售价和库存。 */
@Entity
@Table(name = "barcode_catalog")
public class BarcodeCatalog {

    @Id
    @Column(length = 13, nullable = false)
    private String barcode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long updatedAt;

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

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
