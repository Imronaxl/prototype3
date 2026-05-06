package com.deliveryflow.api.controller;

import com.deliveryflow.api.dto.request.CreateOrderRequest;
import com.deliveryflow.api.dto.response.OrderResponse;
import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.mapper.OrderMapper;
import com.deliveryflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID clientId = UUID.fromString(userDetails.getUsername());
        Order order = orderService.createOrder(
                clientId,
                request.pickupAddress(),
                request.deliveryAddress(),
                request.pickupLat(),
                request.pickupLng(),
                request.deliveryLat(),
                request.deliveryLng(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponse(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        UUID clientId = UUID.fromString(userDetails.getUsername());
        List<Order> orders = orderService.findByClientId(clientId);
        return ResponseEntity.ok(orders.stream().map(orderMapper::toResponse).toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID clientId = UUID.fromString(userDetails.getUsername());
        orderService.cancelOrder(id, clientId);
        return ResponseEntity.noContent().build();
    }
}
