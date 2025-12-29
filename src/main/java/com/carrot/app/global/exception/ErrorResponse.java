package com.carrot.app.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// ???? 이건 뭐길래 이 폴더안에 있는거야
@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private final String message;
    private final int status;
}
