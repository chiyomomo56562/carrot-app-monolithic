package com.carrot.app.domain.chat.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.ApplicationEventPublisher;

import com.carrot.app.domain.chat.document.ChatMessage;
import com.carrot.app.domain.chat.repository.ChatRepository;
import com.carrot.app.domain.chat.repository.ChatRoomRepository;
import com.carrot.app.domain.product.repository.ProductRepository;
import com.carrot.app.domain.user.repository.UserRepository;
import com.carrot.app.domain.chat.dto.ChatRoomResponse;
import com.carrot.app.domain.chat.dto.ChatRoomCreateRequest;
import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.chat.document.ChatRoom;
import com.carrot.app.global.common.CacheKey;
import com.carrot.app.global.exception.ChatRoomNotFoundException;
import com.carrot.app.global.exception.ProductNotFoundException;
import com.carrot.app.global.exception.UserNotFoundException;
import com.carrot.app.infra.s3.S3Service;
import com.carrot.app.domain.chat.dto.ChatMessageRequest;
import com.carrot.app.domain.chat.dto.ChatMessageResponse;
import com.carrot.app.domain.chat.dto.ChatEvent;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatRepository chatRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final S3Service s3Service;
	private final ApplicationEventPublisher eventPublisher;
	private final MongoTemplate mongoTemplate;

	/**
	 * * 채팅방 생성 (Polyglot Persistence: MySQL 검증 -> MongoDB 저장)
	 */
	private final RedisTemplate<String, Object> redisTemplate;

	@Transactional
	public ChatRoomResponse createChatRoom(ChatRoomCreateRequest request, Long buyerId) {

		// 1. MySQL에서 데이터 조회 및 검증
		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new ProductNotFoundException("Product not found"));

		User buyer = userRepository.findById(buyerId)
				.orElseThrow(() -> new UserNotFoundException("Buyer not found"));

		User seller = product.getSeller();

		// 2. MongoDB에서 채팅방 조회 혹은 생성
		ChatRoom chatRoom = chatRoomRepository
				.findByProductIdAndBuyerId(request.getProductId(), buyerId)
				.orElseGet(() -> {
					ChatRoom newRoom = ChatRoom
							.builder()
							.productId(product.getId())
							.sellerId(seller.getId())
							.buyerId(buyer.getId())
							.createdAt(LocalDateTime.now())
							.build();
					return chatRoomRepository.save(newRoom);
				});
		log.info("### ChatRoom created or found: {}", chatRoom.getId());

		// 3. Redis Pre-caching (Member Info for Security)
		String cacheKey = "chatroom:members:" + chatRoom.getId();
		String members = seller.getId() + ":" + buyer.getId();

		redisTemplate.opsForValue().set(cacheKey, members, CacheKey.CHAT_ROOMS_TTL, TimeUnit.SECONDS);

		// 4. Response 생성 (with User Info)
		// profileImageUrl=null인 경우의 처리를 해줘야겠다. User에서
		return ChatRoomResponse.builder()
				.roomId(chatRoom.getId())
				.productId(chatRoom.getProductId())
				.sellerId(seller.getId())
				.sellerNickname(seller.getNickname())
				.sellerProfileImage(seller.getProfileImageUrl())
				.buyerId(buyer.getId())
				.buyerNickname(buyer.getNickname())
				.buyerProfileImage(buyer.getProfileImageUrl())
				.lastMessage(chatRoom.getLastMessage())
				.lastMessageSentAt(chatRoom.getLastMessageSentAt())
				.productTitle(product.getTitle())
				.productPrice(product.getPrice())
				.productThumbnail(product.getThumbnailUrl())
				.build();
	}

	/**
	 * 메시지 전송 (REST API/Multipart 전용)
	 */
	@Transactional
	public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request, MultipartFile image) {
		String imageUrl = null;
		if (image != null && !image.isEmpty()) {
			imageUrl = s3Service.uploadOptimizedImage(image);
			request.setType(ChatMessage.MessageType.IMAGE);
		}

		// ChatRoom 존재 확인
		ChatRoom chatRoom = chatRoomRepository.findById(request.getRoomId())
				.orElseThrow(() -> new ChatRoomNotFoundException("ChatRoom not found"));

		ChatMessage message = ChatMessage.builder()
				.roomId(request.getRoomId())
				.senderId(senderId)
				.content(request.getContent())
				.type(request.getType() != null ? request.getType() : ChatMessage.MessageType.TEXT)
				.imageUrl(imageUrl)
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.build();

		// 1. MongoDB 즉시 저장
		ChatMessage savedMessage = chatRepository.save(message);

		// 2. ChatRoom 최신 메시지 업데이트
		updateChatRoomLastMessage(chatRoom, savedMessage);

		// 3. 비동기 처리를 위한 이벤트 발행 (대상 유저 확인)
		Long recipientId = chatRoom.getSellerId().equals(senderId) ? chatRoom.getBuyerId()
				: chatRoom.getSellerId();

		ChatEvent event = ChatEvent.builder()
				.id(savedMessage.getId())
				.roomId(savedMessage.getRoomId())
				.senderId(savedMessage.getSenderId())
				.recipientId(recipientId)
				.content(savedMessage.getContent())
				.type(savedMessage.getType())
				.imageUrl(savedMessage.getImageUrl())
				.createdAt(savedMessage.getCreatedAt())
				.build();

		log.info("### Publishing event for room: {}", event.roomId());

		eventPublisher.publishEvent(event);

		return ChatMessageResponse.from(savedMessage);
	}

	private void updateChatRoomLastMessage(ChatRoom chatRoom, ChatMessage message) {
		String lastMsgContent = (message.getType() == ChatMessage.MessageType.IMAGE)
				? "사진을 보냈습니다."
				: message.getContent();
		// 1. 조건: 해당 ID를 가진 채팅방
		Query query = new Query(Criteria.where("_id").is(chatRoom.getId()));

		// 2. 변경할 필드만 지정 ($set 연산)
		Update update = new Update()
				.set("lastMessage", lastMsgContent)
				.set("lastMessageSentAt", message.getCreatedAt());
		// 3. 실행: 다른 필드(구매자/판매자 ID 등)는 건드리지 않고 지정한 필드만 원자적으로 수정
		mongoTemplate.updateFirst(query, update, ChatRoom.class);
	}

	/**
	 * 메시지 전송 (WebSocket 전용)
	 */
	@Transactional
	public void sendMessage(ChatMessage message) {
		ChatRoom chatRoom = chatRoomRepository.findById(message.getRoomId())
				.orElseThrow(() -> new ChatRoomNotFoundException("ChatRoom not found"));

		message.setCreatedAt(LocalDateTime.now());

		// 1. MongoDB 즉시 저장
		ChatMessage savedMessage = chatRepository.save(message);

		// 2. ChatRoom 최신 메시지 업데이트
		updateChatRoomLastMessage(chatRoom, savedMessage);

		Long recipientId = chatRoom.getSellerId().equals(savedMessage.getSenderId()) ? chatRoom.getBuyerId()
				: chatRoom.getSellerId();

		ChatEvent event = ChatEvent.builder()
				.id(savedMessage.getId())
				.roomId(savedMessage.getRoomId())
				.senderId(savedMessage.getSenderId())
				.recipientId(recipientId)
				.content(savedMessage.getContent())
				.type(savedMessage.getType())
				.imageUrl(savedMessage.getImageUrl())
				.createdAt(savedMessage.getCreatedAt())
				.build();

		log.info("### Publishing event for room: {}", event.roomId());
		eventPublisher.publishEvent(event);
	}

	/**
	 * 채팅 기록 조회
	 */
	public Slice<ChatMessage> getChatHistory(String roomId, Pageable pageable) {
		return chatRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
	}

	/**
	 * 채팅방 목록 조회
	 */
	public Slice<ChatRoomResponse> getChatRooms(Long userId, Pageable pageable) {
		Slice<ChatRoom> rooms = chatRoomRepository.findBySellerIdOrBuyerIdOrderByLastMessageSentAtDesc(userId,
				userId,
				pageable);

		return rooms.map(room -> {
			User seller = userRepository.findById(room.getSellerId())
					.orElseThrow(() -> new UserNotFoundException("Seller not found"));
			User buyer = userRepository.findById(room.getBuyerId())
					.orElseThrow(() -> new UserNotFoundException("Buyer not found"));

			Product product = productRepository.findById(room.getProductId())
					.orElseThrow(() -> new ProductNotFoundException("Product not found"));

			long unreadCount = chatRepository.countByRoomIdAndSenderIdNotAndIsReadFalse(room.getId(), userId);

			return ChatRoomResponse.builder()
					.roomId(room.getId())
					.productId(room.getProductId())
					.sellerId(seller.getId())
					.sellerNickname(seller.getNickname())
					.sellerProfileImage(seller.getProfileImageUrl())
					.buyerId(buyer.getId())
					.buyerNickname(buyer.getNickname())
					.buyerProfileImage(buyer.getProfileImageUrl())
					.lastMessage(room.getLastMessage())
					.lastMessageSentAt(room.getLastMessageSentAt())
					.productTitle(product.getTitle())
					.productPrice(product.getPrice())
					.productThumbnail(product.getThumbnailUrl())
					.unreadCount(unreadCount)
					.build();
		});
	}

	/**
	 * 읽음 처리 (비동기)
	 */
	@Async
	public void markAsRead(String roomId, Long userId) {
		// 해당 방의 메시지 중 내가 보낸 것이 아닌 메시지를 읽음 처리
		// userId = 현재 접속한 사람 (읽은 사람)
		// senderId != userId AND isRead == false
		chatRepository.updateIsReadByRoomIdAndSenderIdNot(roomId, userId);
	}

	/**
	 * 채팅방 상세 정보 조회
	 */
	public ChatRoomResponse getChatRoom(String roomId) {
		ChatRoom room = chatRoomRepository.findById(roomId)
				.orElseThrow(() -> new ChatRoomNotFoundException("ChatRoom not found"));

		User seller = userRepository.findById(room.getSellerId())
				.orElseThrow(() -> new UserNotFoundException("Seller not found"));
		User buyer = userRepository.findById(room.getBuyerId())
				.orElseThrow(() -> new UserNotFoundException("Buyer not found"));

		Product product = productRepository.findById(room.getProductId())
				.orElseThrow(() -> new ProductNotFoundException("Product not found"));

		return ChatRoomResponse.builder()
				.roomId(room.getId())
				.productId(room.getProductId())
				.sellerId(seller.getId())
				.sellerNickname(seller.getNickname())
				.sellerProfileImage(seller.getProfileImageUrl())
				.buyerId(buyer.getId())
				.buyerNickname(buyer.getNickname())
				.buyerProfileImage(buyer.getProfileImageUrl())
				.lastMessage(room.getLastMessage())
				.lastMessageSentAt(room.getLastMessageSentAt())
				.productTitle(product.getTitle())
				.productPrice(product.getPrice())
				.productThumbnail(product.getThumbnailUrl())
				.build();
	}
}
