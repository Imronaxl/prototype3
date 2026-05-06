package com.deliveryflow.messaging.consumer;

import com.deliveryflow.messaging.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    @KafkaListener(topics = "orders.created", groupId = "order-service-group")
    public void consumeOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event: {}", event);
    }

    @KafkaListener(topics = "orders.status-changed", groupId = "order-service-group")
    public void consumeOrderStatusChanged(com.deliveryflow.messaging.event.OrderStatusChangedEvent event) {
        log.info("Received order status changed event: {}", event);
    }
}
