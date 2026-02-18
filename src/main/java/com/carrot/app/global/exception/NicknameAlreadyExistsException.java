package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class NicknameAlreadyExistsException extends BusinessException {
    public NicknameAlreadyExistsException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
