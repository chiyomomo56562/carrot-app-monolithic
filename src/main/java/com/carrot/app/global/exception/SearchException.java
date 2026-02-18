package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class SearchException extends BusinessException {
    public SearchException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
