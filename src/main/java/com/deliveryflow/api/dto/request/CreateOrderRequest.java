package com.deliveryflow.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotBlank(message = "Pickup address is required")
        String pickupAddress,

        @NotNull(message = "Pickup latitude is required")
        Double pickupLat,

        @NotNull(message = "Pickup longitude is required")
        Double pickupLng,

        @NotBlank(message = "Delivery address is required")
        String deliveryAddress,

        @NotNull(message = "Delivery latitude is required")
        Double deliveryLat,

        @NotNull(message = "Delivery longitude is required")
        Double deliveryLng,

        String description
) {}
