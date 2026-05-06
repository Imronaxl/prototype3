package com.deliveryflow.messaging.producer;

import com.deliveryflow.config.KafkaConfig;
import com.deliveryflow.messaging.event.CourierLocationUpdatedEvent;
import com.deliveryflow.messaging.event.NotificationSendEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(NotificationSendEvent event) {
        kafkaTemplate.send(KafkaConfig.NOTIFICATIONS_SEND, event.userId().toString(), event);
    }

    public void sendLocationUpdate(CourierLocationUpdatedEvent event) {
        kafkaTemplate.send(KafkaConfig.COURIER_LOCATION_UPDATED, event.courierId().toString(), event);
    }
}
