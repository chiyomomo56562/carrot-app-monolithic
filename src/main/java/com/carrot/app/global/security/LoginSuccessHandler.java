package com.carrot.app.global.security;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import com.carrot.app.global.security.refreshToken.RefreshToken;
import com.carrot.app.global.security.refreshToken.RefreshTokenRepository;

import lombok.extern.slf4j.Slf4j;

import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.user.repository.UserRepository;
import com.carrot.app.global.exception.UserNotFoundException;
import com.carrot.app.global.security.refreshToken.JwtUtil;

import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        private final JwtUtil jwtUtil;
        private final RefreshTokenRepository refreshTokenRepository;
        private final UserRepository userRepository;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication) throws IOException {
                String email = authentication.getName();

                // 토큰 발급 및 DB 저장
                String accessToken = jwtUtil.generateAccessToken(email);
                String refreshToken = jwtUtil.generateRefreshToken(email);

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UserNotFoundException("User not found"));
                // RefreshToken 저장
                refreshTokenRepository.save(RefreshToken.builder()
                                .userId(user.getId())
                                .refreshToken(refreshToken)
                                .ttl(jwtUtil.getRefreshExpiration())
                                .build());

                jwtUtil.addCookie(response, "accessToken", accessToken, jwtUtil.getAccessExpiration());
                jwtUtil.addCookie(response, "refreshToken", refreshToken, jwtUtil.getRefreshExpiration());

                getRedirectStrategy().sendRedirect(request, response, "/");
        }
}