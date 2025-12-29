package com.carrot.app.domain.chat.service;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.carrot.app.domain.chat.document.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            log.info("### Redis Subscriber onMessage triggered on topic: {} ###", new String(message.getChannel()));
            // Redis에서 수신한 메시지 역직렬화
            // RedisConfig에서 설정한 serializer 사용
            Object deserialized = redisTemplate.getValueSerializer().deserialize(message.getBody());

            if (deserialized instanceof ChatMessage) {
                ChatMessage chatMessage = (ChatMessage) deserialized;
                log.info("### Redis Subscriber broadcasting to WebSocket: /topic/chat/room/{} ###",
                        chatMessage.getRoomId());

                // WebSocket 구독자에게 메시지 전달
                // /topic/chat/room/{roomId}
                messagingTemplate.convertAndSend("/topic/chat/room/" + chatMessage.getRoomId(), chatMessage);
            } else {
                log.warn("### Redis Subscriber received unknown type: {} ###",
                        deserialized != null ? deserialized.getClass().getName() : "null");
            }
        } catch (Exception e) {
            log.error("### Exception in RedisSubscriber: {} ###", e.getMessage(), e);
        }
    }
}
