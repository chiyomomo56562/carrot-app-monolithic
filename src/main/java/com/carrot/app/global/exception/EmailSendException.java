package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class EmailSendException extends BusinessException {
    public EmailSendException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
