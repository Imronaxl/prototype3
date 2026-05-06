package com.deliveryflow.messaging.consumer;

import com.deliveryflow.infrastructure.redis.CourierGeoRepository;
import com.deliveryflow.messaging.event.CourierLocationUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LocationConsumer {

    private static final Logger log = LoggerFactory.getLogger(LocationConsumer.class);
    private final CourierGeoRepository courierGeoRepository;

    public LocationConsumer(CourierGeoRepository courierGeoRepository) {
        this.courierGeoRepository = courierGeoRepository;
    }

    @KafkaListener(topics = "courier.location-updated", groupId = "location-service-group")
    public void consumeLocationUpdate(CourierLocationUpdatedEvent event) {
        log.info("Received location update for courier {}: {}", event.courierId(), event);
        courierGeoRepository.updateCourierLocation(
                event.courierId().toString(),
                new Point(event.longitude(), event.latitude())
        );
    }
}
