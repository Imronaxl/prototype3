package com.deliveryflow.service;

import com.deliveryflow.messaging.event.NotificationSendEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(NotificationSendEvent event) {
        kafkaTemplate.send("notifications.send", event.userId().toString(), event);
    }
}
