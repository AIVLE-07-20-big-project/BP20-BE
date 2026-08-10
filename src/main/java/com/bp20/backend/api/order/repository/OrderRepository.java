package com.bp20.backend.api.order.repository;

import com.bp20.backend.api.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStoreIdOrderByOrderedDateDesc(Long storeId);

    Optional<Order> findByStoreIdAndSourceOrderId(Long storeId, Long sourceOrderId);

    void deleteAllByStoreId(Long storeId);

    long countByStoreId(Long storeId);
}
