package com.carrot.app.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.carrot.app.global.security.refreshToken.RefreshTokenRepository;
import com.carrot.app.global.security.refreshToken.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // 쿠키에서 refreshToken 추출
        log.info("### CustomLogoutHandler");
        String refreshToken = jwtUtil.resolveToken(request, "refreshToken");

        if (refreshToken != null) {
            // Redis/DB에서 해당 토큰 삭제
            refreshTokenRepository.findByRefreshToken(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }
}