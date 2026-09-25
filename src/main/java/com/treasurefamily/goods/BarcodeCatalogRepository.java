package com.treasurefamily.goods;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BarcodeCatalogRepository extends JpaRepository<BarcodeCatalog, String> {
}
