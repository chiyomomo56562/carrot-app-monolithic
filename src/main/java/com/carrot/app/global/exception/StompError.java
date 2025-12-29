package com.carrot.app.global.exception;

public class StompError extends RuntimeException {
    public StompError(String message) {
        super(message);
    }
}
