package com.deliveryflow.service;

import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.domain.enums.OrderStatus;
import com.deliveryflow.domain.repository.OrderRepository;
import com.deliveryflow.infrastructure.redis.CourierGeoRepository;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CourierService {

    private final OrderRepository orderRepository;
    private final CourierGeoRepository courierGeoRepository;
    private final OrderService orderService;

    public CourierService(
            OrderRepository orderRepository,
            CourierGeoRepository courierGeoRepository,
            OrderService orderService) {
        this.orderRepository = orderRepository;
        this.courierGeoRepository = courierGeoRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public List<Order> findAvailableOrders() {
        return orderRepository.findByStatus(OrderStatus.CREATED);
    }

    @Transactional(readOnly = true)
    public List<GeoResult<String>> findNearbyCouriers(double longitude, double latitude, double radiusKm) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = courierGeoRepository.findNearbyCouriers(
                longitude,
                latitude,
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        if (results == null) {
            return List.of();
        }

        return results.getContent();
    }
}
