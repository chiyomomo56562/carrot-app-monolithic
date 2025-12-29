package com.carrot.app.infra.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.messaging.simp.config.ChannelRegistration;

import com.carrot.app.infra.websocket.StompHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    // 메시지 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 구독(sub) : /topic (pub/sub), /queue (1:1)
        config.enableSimpleBroker("/topic", "/queue");
        // 발행(pub) : /app
        config.setApplicationDestinationPrefixes("/app");
    }

    // 인바운드 채널 설정
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 웹소켓 연결 시점에 JWT 토큰 유효성을 검증하거나 권한을 체크하기 위한 인터셉터
        registration.interceptors(stompHandler);
    }

    // STOMP 엔드포인트 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ws://localhost:8080/ws-chat
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // 접속 가능한 도메인(수정하는게 좋다.)
                .addInterceptors(new HttpSessionHandshakeInterceptor()) // HTTP 쿠키정보를 웹소켓 세션 안으로 복사
                .withSockJS();
    }
}
