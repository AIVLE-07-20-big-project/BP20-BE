package com.bp20.backend.api.order.repository;

import com.bp20.backend.api.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    void deleteAllByOrder_Store_Id(Long storeId);

    long countByOrder_Store_Id(Long storeId);
}
