package com.carrot.app.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.carrot.app.global.security.refreshToken.JwtUtil;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("### JWT Authentication Filter");

        String accessToken = jwtUtil.resolveToken(request, "accessToken");
        String refreshToken = jwtUtil.resolveToken(request, "refreshToken");

        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            // 1. Access Token이 유효한 경우
            this.setAuthentication(accessToken);
            log.info("### Access Token is valid");
        } else if (refreshToken != null && jwtUtil.validateToken(refreshToken)) {
            // 2. Access Token은 만료됐지만 Refresh Token이 유효한 경우 (재발급)
            String email = jwtUtil.getEmailFromToken(refreshToken);
            String newAccessToken = jwtUtil.generateAccessToken(email);

            // 새 Access Token을 쿠키에 저장
            jwtUtil.addCookie(response, "accessToken", newAccessToken, 1800000); // 30분
            this.setAuthentication(newAccessToken);
        }
        log.info("### JWT Authentication Filter end");

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String token) {
        String email = jwtUtil.getEmailFromToken(token);
        MDC.put("userId", email);
        Authentication authentication = jwtUtil.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
