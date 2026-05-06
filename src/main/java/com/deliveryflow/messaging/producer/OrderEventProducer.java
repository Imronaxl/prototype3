package com.deliveryflow.messaging.producer;

import com.deliveryflow.config.KafkaConfig;
import com.deliveryflow.messaging.event.OrderCreatedEvent;
import com.deliveryflow.messaging.event.OrderStatusChangedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(KafkaConfig.ORDERS_CREATED, event.orderId().toString(), event);
    }

    public void sendOrderStatusChanged(OrderStatusChangedEvent event) {
        kafkaTemplate.send(KafkaConfig.ORDERS_STATUS_CHANGED, event.orderId().toString(), event);
    }
}
