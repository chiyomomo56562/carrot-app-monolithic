package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class ForbidenException extends BusinessException {
    public ForbidenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
