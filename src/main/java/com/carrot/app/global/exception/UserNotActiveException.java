package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class UserNotActiveException extends BusinessException {
    public UserNotActiveException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
