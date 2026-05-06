package com.deliveryflow.api.exception;

public class UserNotFoundException extends AppException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }

    public static UserNotFoundException byId(java.util.UUID id) {
        return new UserNotFoundException("User not found with id: " + id);
    }
}
