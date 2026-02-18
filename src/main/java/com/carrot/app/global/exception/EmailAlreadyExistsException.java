package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BusinessException {
    public EmailAlreadyExistsException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
