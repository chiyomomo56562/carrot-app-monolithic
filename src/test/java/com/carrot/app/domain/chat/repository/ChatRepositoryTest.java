package com.carrot.app.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.carrot.app.domain.chat.document.ChatMessage;
import com.carrot.app.domain.chat.document.ChatMessage.MessageType;

@DataMongoTest
class ChatRepositoryTest {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        chatRepository.deleteAll();
    }

    @Test
    @DisplayName("메시지 저장 및 조회 성공")
    void save_Success() {
        // given
        ChatMessage message = ChatMessage.builder()
                .roomId("room1")
                .senderId(1L)
                .content("Hello")
                .type(MessageType.TEXT)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // when
        ChatMessage saved = chatRepository.save(message);

        // then
        assertThat(saved.getId()).isNotNull();
        ChatMessage found = chatRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getContent()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("방 번호로 메시지 조회 및 정렬 (인덱스 검증)")
    void findByRoomId_SortCheck() {
        // given
        String roomId = "room1";
        for (int i = 0; i < 5; i++) {
            ChatMessage msg = ChatMessage.builder()
                    .roomId(roomId)
                    .senderId(1L)
                    .content("Message " + i)
                    .createdAt(LocalDateTime.now().plusSeconds(i)) // Ascending time
                    .build();
            chatRepository.save(msg);
        }

        // when
        Pageable pageable = PageRequest.of(0, 10);
        // Repository method enforces 'OrderByCreatedAtDesc'
        Slice<ChatMessage> result = chatRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        // then
        List<ChatMessage> content = result.getContent();
        assertThat(content).hasSize(5);
        // Verify Descending Order
        assertThat(content.get(0).getContent()).isEqualTo("Message 4");
        assertThat(content.get(4).getContent()).isEqualTo("Message 0");
    }

    @Test
    @DisplayName("읽음 처리 업데이트 (@Query) 동작 확인")
    void updateIsRead_Success() {
        // given
        String roomId = "room1";
        Long myId = 1L;
        Long otherId = 2L;

        // Message 1: Sent by other, unread (Should be updated)
        chatRepository.save(ChatMessage.builder().roomId(roomId).senderId(otherId).isRead(false).build());
        // Message 2: Sent by me, unread (Should NOT be updated)
        chatRepository.save(ChatMessage.builder().roomId(roomId).senderId(myId).isRead(false).build());
        // Message 3: Sent by other, already read (Should NOT be updated - though result
        // is same)
        chatRepository.save(ChatMessage.builder().roomId(roomId).senderId(otherId).isRead(true).build());

        // when
        // I am reading the room, so messages sent by 'other' (sender != myId) should
        // become read
        chatRepository.updateIsReadByRoomIdAndSenderIdNot(roomId, myId);

        // then
        long unreadCount = chatRepository.countByRoomIdAndSenderIdNotAndIsReadFalse(roomId, myId);
        assertThat(unreadCount).isEqualTo(0); // All messages from others should be read

        // Check if my message is still unread (optional, but validates sender check)
        // Since custom query only updates sender != myId, my messages remain untouched?
        // Actually logic: "senderId != userId" means "messages sent by others".
        // My message has senderId == myId, so it is filtered OUT. status isRead should
        // remain false?
        // Let's verify.
        List<ChatMessage> myMessages = chatRepository.findAll();
        long myUnread = myMessages.stream()
                .filter(m -> m.getSenderId().equals(myId))
                .filter(m -> !m.getIsRead())
                .count();
        assertThat(myUnread).isEqualTo(1);
    }
}
