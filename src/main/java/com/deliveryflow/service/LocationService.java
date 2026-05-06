package com.deliveryflow.service;

import com.deliveryflow.domain.entity.User;
import com.deliveryflow.domain.enums.UserRole;
import com.deliveryflow.domain.repository.UserRepository;
import com.deliveryflow.infrastructure.redis.CourierGeoRepository;
import com.deliveryflow.messaging.event.CourierLocationUpdatedEvent;
import com.deliveryflow.messaging.producer.NotificationProducer;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LocationService {

    private final CourierGeoRepository courierGeoRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    public LocationService(
            CourierGeoRepository courierGeoRepository,
            UserRepository userRepository,
            NotificationProducer notificationProducer) {
        this.courierGeoRepository = courierGeoRepository;
        this.userRepository = userRepository;
        this.notificationProducer = notificationProducer;
    }

    @Transactional
    public void updateCourierLocation(UUID courierId, double latitude, double longitude) {
        User courier = userRepository.findById(courierId)
                .filter(user -> user.getRole() == UserRole.COURIER)
                .orElseThrow(() -> new IllegalArgumentException("User is not a courier"));

        courierGeoRepository.updateCourierLocation(courierId.toString(), new Point(longitude, latitude));

        notificationProducer.sendNotification(new CourierLocationUpdatedEvent(
                courierId,
                latitude,
                longitude,
                Instant.now()
        ));
    }
}
