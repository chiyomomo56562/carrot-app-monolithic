package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class ImageProcessFailedException extends BusinessException {
    public ImageProcessFailedException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
