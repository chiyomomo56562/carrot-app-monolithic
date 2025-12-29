package com.carrot.app.infra.websocket;

import com.carrot.app.domain.chat.repository.ChatRoomRepository;
import com.carrot.app.global.security.refreshToken.JwtUtil;
import com.carrot.app.global.exception.StompError;
import com.carrot.app.global.security.CustomUserDetails;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;
import com.carrot.app.domain.chat.service.ChatService;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // 보안 필터들 보다는 뒤에, 하지만 비지니스 로직보다는 앞에
public class StompHandler implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final ChatRoomRepository chatRoomRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Lazy
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.info("### STOMP preSend message={} ###", message);
        try {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null)
                return message;

            StompCommand command = accessor.getCommand();
            String destination = accessor.getDestination();
            log.info("### STOMP Frame: cmd={}, dest={}, user={} ###", command, destination, accessor.getUser());

            if (StompCommand.CONNECT.equals(command)) {
                // 1. 이미 핸드쉐이크 단계에서 인증된 유저 정보가 있는지 확인
                Authentication existingAuth = (Authentication) accessor.getUser();

                if (existingAuth != null && existingAuth.isAuthenticated()) {
                    log.info("### CONNECT: User already authenticated via Session/Handshake: {} ###",
                            existingAuth.getName());
                    return message; // 이미 인증되었으니 토큰 추출 로직을 건너뜀
                }

                // 2. 만약 인증 정보가 없다면 그때 토큰 추출 시도 (모바일 등 세션 없는 환경 대비)
                String cookieHeader = accessor.getFirstNativeHeader("Cookie");
                String token = extractTokenFromCookie(cookieHeader, "accessToken");

                log.info("### CONNECT token extraction: token exists={} ###", token != null);

                if (token != null) {
                    boolean isValid = jwtUtil.validateToken(token);
                    log.info("### CONNECT token validation: isValid={} ###", isValid);
                    if (isValid) {
                        Authentication authentication = jwtUtil.getAuthentication(token);
                        accessor.setUser(authentication);
                        log.info("WebSocket Authenticated: {}", jwtUtil.getEmailFromToken(token));
                    } else {
                        log.warn("### CONNECT failed: Invalid token ###");
                        throw new StompError("유효하지 않은 토큰입니다.");
                    }
                } else {
                    log.warn("### CONNECT failed: No token found in Cookie or Authorization header ###");
                    throw new StompError("인증 토큰이 없습니다.");
                }
            } else if (StompCommand.SUBSCRIBE.equals(command)) {
                if (destination != null && destination.startsWith("/topic/chat/room/")) {
                    String roomId = destination.substring("/topic/chat/room/".length());

                    Authentication auth = (Authentication) accessor.getUser();
                    if (auth == null)
                        throw new StompError("인증되지 않은 사용자입니다.");

                    CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                    // 1:1 채팅방 멤버십 확인 (Redis Cache-Aside)
                    verifyChatRoomMembership(roomId, userDetails.getId());

                    // 읽음 처리 (Async)
                    chatService.markAsRead(roomId, userDetails.getId());
                }
            } else if (StompCommand.SEND.equals(command)) {
                Authentication auth = (Authentication) accessor.getUser();
                if (auth != null) {
                    CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                    log.info("### STOMP SEND: user={}, destination={} ###", userDetails.getId(), destination);
                } else {
                    log.warn("### STOMP SEND: Unauthenticated user, destination={} ###", destination);
                }
            } else if (StompCommand.DISCONNECT.equals(command)) {
                Authentication auth = (Authentication) accessor.getUser();
                if (auth != null) {
                    CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                    log.info("### STOMP DISCONNECT: user={} ###", userDetails.getId());
                } else {
                    log.warn("### STOMP DISCONNECT: Unauthenticated user ###");
                }
            }

            return message;
        } catch (Exception e) {
            log.error("### ERROR in StompHandler preSend: {} ###", e.getMessage(), e);
            throw e;
        }
    }

    private String extractTokenFromCookie(String cookieHeader, String cookieName) {
        if (cookieHeader == null)
            return null;
        return Arrays.stream(cookieHeader.split(";"))
                .map(String::trim)
                .filter(c -> c.startsWith(cookieName + "="))
                .map(c -> c.substring((cookieName + "=").length()))
                .findFirst()
                .orElse(null);
    }

    private void verifyChatRoomMembership(String roomId, Long userId) {
        String cacheKey = "chatroom:members:" + roomId;
        Object cachedMembers = redisTemplate.opsForValue().get(cacheKey);

        if (cachedMembers != null) {
            String[] split = ((String) cachedMembers).split(":");
            Long sellerId = Long.parseLong(split[0]);
            Long buyerId = Long.parseLong(split[1]);

            if (!userId.equals(sellerId) && !userId.equals(buyerId)) {
                throw new StompError("해당 채팅방의 멤버가 아닙니다.");
            }
            return;
        }

        chatRoomRepository.findById(roomId).ifPresentOrElse(chatRoom -> {
            if (!userId.equals(chatRoom.getSellerId()) && !userId.equals(chatRoom.getBuyerId())) {
                throw new StompError("해당 채팅방의 멤버가 아닙니다.");
            }
            String value = chatRoom.getSellerId() + ":" + chatRoom.getBuyerId();
            redisTemplate.opsForValue().set(cacheKey, value, 1, TimeUnit.HOURS);
        }, () -> {
            throw new StompError("존재하지 않는 채팅방입니다.");
        });
    }
}
