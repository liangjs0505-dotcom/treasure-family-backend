package com.treasurefamily.goods;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsRepository extends JpaRepository<Goods, String> {

    List<Goods> findByOwnerId(String ownerId);

    Optional<Goods> findByIdAndOwnerId(String id, String ownerId);

    boolean existsByIdAndOwnerId(String id, String ownerId);

    void deleteByOwnerId(String ownerId);
}
