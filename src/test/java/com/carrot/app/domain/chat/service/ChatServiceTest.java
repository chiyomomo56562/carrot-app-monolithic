package com.carrot.app.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.multipart.MultipartFile;

import com.carrot.app.domain.chat.document.ChatMessage;
import com.carrot.app.domain.chat.document.ChatRoom;
import com.carrot.app.domain.chat.dto.ChatEvent;
import com.carrot.app.domain.chat.dto.ChatRoomCreateRequest;
import com.carrot.app.domain.chat.dto.ChatRoomResponse;
import com.carrot.app.domain.chat.dto.ChatMessageRequest;
import com.carrot.app.domain.chat.repository.ChatRepository;
import com.carrot.app.domain.chat.repository.ChatRoomRepository;
import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.product.repository.ProductRepository;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.user.repository.UserRepository;
import com.carrot.app.global.exception.ChatRoomNotFoundException;
import com.carrot.app.global.exception.ProductNotFoundException;
import com.carrot.app.global.exception.UserNotFoundException;
import com.carrot.app.infra.s3.S3Service;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3Service s3Service;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Test
    @DisplayName("채팅방 생성 성공 (신규 생성)")
    void createChatRoom_Success_New() {
        // given
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long productId = 10L;

        Product product = Product.builder().id(productId).seller(User.builder().id(sellerId).nickname("Seller").build())
                .title("Product").price(100).build();
        User buyer = User.builder().id(buyerId).nickname("Buyer").build();

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(userRepository.findById(buyerId)).willReturn(Optional.of(buyer));
        // Room not found logic
        given(chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId)).willReturn(Optional.empty());

        ChatRoom newRoom = ChatRoom.builder().id("room1").productId(productId).sellerId(sellerId).buyerId(buyerId)
                .build();
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(newRoom);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(productId);
        ChatRoomResponse response = chatService.createChatRoom(request, buyerId);

        // then
        assertThat(response.getRoomId()).isEqualTo("room1");
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(redisTemplate.opsForValue()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 유저 없음")
    void createChatRoom_Fail_UserNotFound() {
        // given
        given(productRepository.findById(anyLong())).willReturn(Optional.of(Product.builder().build()));
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.createChatRoom(new ChatRoomCreateRequest(1L), 1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("메시지 전송 (REST) 성공 - S3 업로드 및 이벤트 발행")
    void sendMessage_Rest_Success() {
        // given
        Long senderId = 1L;
        String roomId = "room1";
        MultipartFile image = null; // Mock if needed, tested mainly in logic path

        ChatMessageRequest request = new ChatMessageRequest();
        request.setRoomId(roomId);
        request.setContent("Hi");

        ChatRoom chatRoom = ChatRoom.builder().id(roomId).sellerId(1L).buyerId(2L).build();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));

        ChatMessage savedMessage = ChatMessage.builder().id("msg1").roomId(roomId).content("Hi").senderId(senderId)
                .createdAt(LocalDateTime.now()).build();
        given(chatRepository.save(any(ChatMessage.class))).willReturn(savedMessage);

        // when
        chatService.sendMessage(senderId, request, null);

        // then
        verify(chatRepository).save(any(ChatMessage.class));
        verify(mongoTemplate).updateFirst(any(), any(), eq(ChatRoom.class));
        verify(eventPublisher).publishEvent(any(ChatEvent.class));
    }

    @Test
    @DisplayName("메시지 전송 (REST) 실패 - S3 에러 시 DB 저장 안함")
    void sendMessage_Fail_S3Error() {
        // given
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        given(image.isEmpty()).willReturn(false);
        doThrow(new RuntimeException("S3 Error")).when(s3Service).uploadOptimizedImage(any());

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(1L, new ChatMessageRequest(), image))
                .isInstanceOf(RuntimeException.class);

        verify(chatRepository, org.mockito.Mockito.times(0)).save(any());
    }

    @Test
    @DisplayName("읽음 처리 호출")
    void markAsRead_Success() {
        // when
        chatService.markAsRead("room1", 1L);

        // then
        verify(chatRepository).updateIsReadByRoomIdAndSenderIdNot("room1", 1L);
    }
}
