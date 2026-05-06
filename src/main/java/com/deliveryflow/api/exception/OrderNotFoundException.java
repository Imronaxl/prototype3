package com.deliveryflow.api.exception;

public class OrderNotFoundException extends AppException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public static OrderNotFoundException byId(java.util.UUID id) {
        return new OrderNotFoundException("Order not found with id: " + id);
    }
}
