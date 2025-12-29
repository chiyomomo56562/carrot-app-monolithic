package com.carrot.app.domain.chat.service;

import com.carrot.app.domain.chat.document.ChatMessage;

import com.carrot.app.domain.chat.dto.ChatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    // 이벤트 발행 토픽을 잘못 설정한 건지 이벤트 리스너가 잘못된건지
    // 레디스 설정이 잘못된건지 정확하게 파악은 안되는데
    // 메시지가 제대로 실시간 전달이 안되는듯.
    // Mongodb에는 문제없이 들어가는데 말이야
    private final RedisPublisher redisPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.properties.topic.notification-request}")
    private String notificationTopic;

    // phase를 AFTER_COMMIT으로 설정하면 DB 저장이 성공했을 때만 실행됩니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatEvent(ChatEvent event) {
        log.info("### ChatEvent received: {}", event);
        ChatMessage message = ChatMessage.builder()
                .id(event.id()) // event.id()가 String이라면 toString() 불필요
                .roomId(event.roomId())
                .senderId(event.senderId())
                .content(event.content())
                .type(event.type())
                .imageUrl(event.imageUrl())
                .isRead(false)
                .createdAt(event.createdAt())
                .build();

        redisPublisher.publish(new ChannelTopic("chat:room:" + event.roomId()), message);
        kafkaTemplate.send(notificationTopic, event);
    }
}
