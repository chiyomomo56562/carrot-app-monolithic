package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class TokenException extends BusinessException {
    public TokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
