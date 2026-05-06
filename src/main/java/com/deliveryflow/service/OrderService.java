package com.deliveryflow.service;

import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.domain.entity.OrderStatusHistory;
import com.deliveryflow.domain.enums.OrderStatus;
import com.deliveryflow.domain.repository.OrderRepository;
import com.deliveryflow.domain.repository.OrderStatusHistoryRepository;
import com.deliveryflow.domain.repository.UserRepository;
import com.deliveryflow.api.exception.InvalidStatusTransitionException;
import com.deliveryflow.api.exception.OrderNotFoundException;
import com.deliveryflow.api.exception.UserNotFoundException;
import com.deliveryflow.messaging.event.OrderCreatedEvent;
import com.deliveryflow.messaging.event.OrderStatusChangedEvent;
import com.deliveryflow.messaging.producer.OrderEventProducer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OrderEventProducer orderEventProducer;
    private final RedissonClient redissonClient;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            OrderEventProducer orderEventProducer,
            RedissonClient redissonClient) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.orderEventProducer = orderEventProducer;
        this.redissonClient = redissonClient;
    }

    @Transactional
    public Order createOrder(UUID clientId, String pickupAddress, String deliveryAddress,
                             double pickupLat, double pickupLng, double deliveryLat, double deliveryLng,
                             String description) {
        userRepository.findById(clientId).orElseThrow(() -> new UserNotFoundException(clientId));

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .status(OrderStatus.CREATED)
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .pickupLat(pickupLat)
                .pickupLng(pickupLng)
                .deliveryLat(deliveryLat)
                .deliveryLng(deliveryLng)
                .description(description)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .id(UUID.randomUUID())
                .orderId(savedOrder.getId())
                .status(OrderStatus.CREATED)
                .changedAt(Instant.now())
                .changedById(clientId)
                .build();
        statusHistoryRepository.save(history);

        orderEventProducer.sendOrderCreated(new OrderCreatedEvent(
                savedOrder.getId(),
                clientId,
                pickupAddress,
                deliveryAddress,
                Instant.now()
        ));

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order findById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> findByClientId(UUID clientId) {
        return orderRepository.findByClientId(clientId);
    }

    @Transactional
    public Order cancelOrder(UUID orderId, UUID clientId) {
        RLock lock = redissonClient.getLock("order:lock:" + orderId);
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    Order order = findById(orderId);

                    if (!order.getClientId().equals(clientId)) {
                        throw new IllegalArgumentException("Only order creator can cancel");
                    }

                    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.ASSIGNED) {
                        throw new InvalidStatusTransitionException(order.getStatus(), OrderStatus.CANCELLED);
                    }

                    updateStatus(order, OrderStatus.CANCELLED, clientId);
                    return order;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new IllegalStateException("Could not acquire lock for order: " + orderId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock interrupted for order: " + orderId, e);
        }
    }

    @Transactional
    public Order assignCourier(UUID orderId, UUID courierId) {
        RLock lock = redissonClient.getLock("order:lock:" + orderId);
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    Order order = findById(orderId);
                    userRepository.findById(courierId).orElseThrow(() -> new UserNotFoundException(courierId));

                    if (order.getStatus() != OrderStatus.CREATED) {
                        throw new InvalidStatusTransitionException(order.getStatus(), OrderStatus.ASSIGNED);
                    }

                    order.setCourierId(courierId);
                    updateStatus(order, OrderStatus.ASSIGNED, courierId);
                    return order;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new IllegalStateException("Could not acquire lock for order: " + orderId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock interrupted for order: " + orderId, e);
        }
    }

    @Transactional
    public Order updateStatus(UUID orderId, OrderStatus newStatus, UUID changedById) {
        RLock lock = redissonClient.getLock("order:lock:" + orderId);
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    Order order = findById(orderId);
                    updateStatus(order, newStatus, changedById);
                    return order;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new IllegalStateException("Could not acquire lock for order: " + orderId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock interrupted for order: " + orderId, e);
        }
    }

    private void updateStatus(Order order, OrderStatus newStatus, UUID changedById) {
        OrderStatus oldStatus = order.getStatus();
        validateStatusTransition(oldStatus, newStatus);

        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .id(UUID.randomUUID())
                .orderId(order.getId())
                .status(newStatus)
                .changedAt(Instant.now())
                .changedById(changedById)
                .build();
        statusHistoryRepository.save(history);

        orderEventProducer.sendOrderStatusChanged(new OrderStatusChangedEvent(
                order.getId(),
                oldStatus,
                newStatus,
                changedById,
                Instant.now()
        ));
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (from) {
            case CREATED -> to == OrderStatus.ASSIGNED || to == OrderStatus.CANCELLED;
            case ASSIGNED -> to == OrderStatus.PICKED_UP || to == OrderStatus.CANCELLED;
            case PICKED_UP -> to == OrderStatus.IN_TRANSIT;
            case IN_TRANSIT -> to == OrderStatus.DELIVERED;
            default -> false;
        };

        if (!valid) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getStatusHistory(UUID orderId) {
        return statusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId);
    }
}
