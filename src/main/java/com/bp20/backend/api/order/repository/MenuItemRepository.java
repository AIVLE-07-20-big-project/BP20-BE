package com.bp20.backend.api.order.repository;

import com.bp20.backend.api.order.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByStoreId(Long storeId);

    Optional<MenuItem> findByStoreIdAndSourceProductId(Long storeId, Long sourceProductId);

    void deleteAllByStoreId(Long storeId);

    long countByStoreId(Long storeId);
}
