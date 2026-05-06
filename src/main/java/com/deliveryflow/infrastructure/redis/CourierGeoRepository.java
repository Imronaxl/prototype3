package com.deliveryflow.infrastructure.redis;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CourierGeoRepository {

    private static final String COURIERS_GEO_KEY = "couriers:geo";
    private final GeoOperations<String, String> geoOperations;
    private final RedisTemplate<String, String> redisTemplate;

    public CourierGeoRepository(
            GeoOperations<String, String> geoOperations,
            RedisTemplate<String, String> redisTemplate) {
        this.geoOperations = geoOperations;
        this.redisTemplate = redisTemplate;
    }

    public void updateCourierLocation(String courierId, org.springframework.data.geo.Point point) {
        geoOperations.add(COURIERS_GEO_KEY, point, courierId);
    }

    public GeoResults<RedisGeoCommands.GeoLocation<String>> findNearbyCouriers(
            double longitude,
            double latitude,
            Distance distance) {
        org.springframework.data.geo.Point point = new org.springframework.data.geo.Point(longitude, latitude);
        return geoOperations.radius(COURIERS_GEO_KEY, point, distance);
    }

    public Boolean removeCourier(String courierId) {
        return redisTemplate.opsForSet().remove(COURIERS_GEO_KEY, courierId);
    }
}
