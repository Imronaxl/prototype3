package com.deliveryflow.service;

import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.domain.entity.User;
import com.deliveryflow.domain.enums.OrderStatus;
import com.deliveryflow.domain.enums.UserRole;
import com.deliveryflow.domain.repository.OrderRepository;
import com.deliveryflow.domain.repository.UserRepository;
import com.deliveryflow.api.exception.InvalidStatusTransitionException;
import com.deliveryflow.api.exception.OrderNotFoundException;
import com.deliveryflow.api.exception.UserNotFoundException;
import com.deliveryflow.messaging.producer.OrderEventProducer;
import com.deliveryflow.infrastructure.redis.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private DistributedLockService distributedLockService;

    @InjectMocks
    private OrderService orderService;

    private User client;
    private User courier;
    private Order order;
    private UUID orderId;
    private UUID clientId;
    private UUID courierId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        courierId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        client = User.builder()
                .id(clientId)
                .email("client@test.com")
                .name("Client Name")
                .role(UserRole.CLIENT)
                .build();

        courier = User.builder()
                .id(courierId)
                .email("courier@test.com")
                .name("Courier Name")
                .role(UserRole.COURIER)
                .build();

        order = Order.builder()
                .id(orderId)
                .client(client)
                .status(OrderStatus.CREATED)
                .pickupAddress("Pickup St 1")
                .deliveryAddress("Delivery St 1")
                .pickupLat(55.75)
                .pickupLng(37.61)
                .deliveryLat(55.76)
                .deliveryLng(37.62)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create order successfully and send event")
    void createOrder_Success() {
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        var request = new com.deliveryflow.api.dto.request.CreateOrderRequest(
                "Pickup St 1", "Delivery St 1",
                55.75, 37.61, 55.76, 37.62, "Test order"
        );

        var result = orderService.createOrder(clientId, request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        verify(orderRepository).save(any(Order.class));
        verify(orderEventProducer).sendOrderCreatedEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when client not found")
    void createOrder_ClientNotFound() {
        when(userRepository.findById(clientId)).thenReturn(Optional.empty());

        var request = new com.deliveryflow.api.dto.request.CreateOrderRequest(
                "Pickup", "Delivery", 55.0, 37.0, 55.0, 37.0, null
        );

        assertThatThrownBy(() -> orderService.createOrder(clientId, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should assign courier to order successfully")
    void acceptOrder_Success() {
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(courierId)).thenReturn(Optional.of(courier));
        when(distributedLockService.lock(anyString(), anyLong())).thenReturn(true);
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(distributedLockService).executeWithLock(anyString(), anyLong(), any(Runnable.class));

        orderService.acceptOrder(orderId, courierId);

        assertThat(order.getCourier()).isEqualTo(courier);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
        verify(orderRepository).save(order);
        verify(orderEventProducer).sendOrderStatusChangedEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when assigning to non-CREATED order")
    void acceptOrder_InvalidStatus() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.acceptOrder(orderId, courierId))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Should update status from ASSIGNED to PICKED_UP")
    void updateStatus_AssignedToPickedUp() {
        order.setStatus(OrderStatus.ASSIGNED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(distributedLockService.lock(anyString(), anyLong())).thenReturn(true);
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(distributedLockService).executeWithLock(anyString(), anyLong(), any(Runnable.class));

        orderService.updateOrderStatus(orderId, OrderStatus.PICKED_UP, courierId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        verify(orderEventProducer).sendOrderStatusChangedEvent(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid status transition")
    void updateStatus_InvalidTransition() {
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED, clientId))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Cannot transition from CREATED to DELIVERED");
    }

    @Test
    @DisplayName("Should cancel order in CREATED status")
    void cancelOrder_Success() {
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(distributedLockService.lock(anyString(), anyLong())).thenReturn(true);
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(distributedLockService).executeWithLock(anyString(), anyLong(), any(Runnable.class));

        orderService.cancelOrder(orderId, clientId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should not allow cancelling DELIVERED order")
    void cancelOrder_AlreadyDelivered() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, clientId))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
