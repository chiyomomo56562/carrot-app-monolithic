package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class StompError extends BusinessException {
    public StompError(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
