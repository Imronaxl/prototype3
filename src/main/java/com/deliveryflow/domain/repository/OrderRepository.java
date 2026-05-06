package com.deliveryflow.domain.repository;

import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByClientId(UUID clientId);

    List<Order> findByCourierId(UUID courierId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCourierIdIsNull();
}
