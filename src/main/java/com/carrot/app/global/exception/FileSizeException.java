package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class FileSizeException extends BusinessException {
    public FileSizeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
