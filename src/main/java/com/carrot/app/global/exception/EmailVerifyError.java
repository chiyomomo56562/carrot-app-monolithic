package com.carrot.app.global.exception;

public class EmailVerifyError extends RuntimeException {
    public EmailVerifyError(String message) {
        super(message);
    }
}
