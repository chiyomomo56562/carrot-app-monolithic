package com.carrot.app.infra.email;

public record EmailVerifyEvent(String email, String token) {

}
