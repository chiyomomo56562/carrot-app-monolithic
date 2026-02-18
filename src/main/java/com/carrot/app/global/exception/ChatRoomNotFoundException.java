package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

public class ChatRoomNotFoundException extends BusinessException {
    public ChatRoomNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
