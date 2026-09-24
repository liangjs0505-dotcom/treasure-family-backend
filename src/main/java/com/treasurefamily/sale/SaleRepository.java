package com.treasurefamily.sale;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, String> {

    List<Sale> findByOwnerIdAndCreatedAtGreaterThanEqual(String ownerId, Long createdAt);

    List<Sale> findByOwnerIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String ownerId, Long start, Long end);
}
