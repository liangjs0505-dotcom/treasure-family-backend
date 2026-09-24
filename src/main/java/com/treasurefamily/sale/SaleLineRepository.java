package com.treasurefamily.sale;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleLineRepository extends JpaRepository<SaleLine, String> {

    List<SaleLine> findByOwnerIdAndSaleIdIn(String ownerId, Collection<String> saleIds);
}
