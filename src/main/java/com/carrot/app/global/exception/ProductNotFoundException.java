package com.carrot.app.global.exception;

import org.springframework.http.HttpStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
