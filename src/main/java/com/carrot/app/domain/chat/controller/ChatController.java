package com.carrot.app.domain.chat.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.carrot.app.domain.chat.document.ChatMessage;
import com.carrot.app.domain.chat.service.ChatService;

import com.carrot.app.domain.chat.dto.ChatRoomResponse;
import com.carrot.app.domain.chat.dto.ChatRoomCreateRequest;
import com.carrot.app.domain.chat.dto.ChatMessageRequest;
import com.carrot.app.domain.chat.dto.ChatMessageResponse;
import com.carrot.app.global.security.CustomUserDetails;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final org.springframework.messaging.simp.SimpMessageSendingOperations messagingTemplate;

    // WebSocket 메시지 처리 (text 전용)
    @MessageMapping("/message")
    public void message(ChatMessage message, Principal principal) {
        if (principal == null)
            return;

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) token.getPrincipal();

        message.setSenderId(userDetails.getId());
        chatService.sendMessage(message);
    }

    // WebSocket 실시간 읽음 신호 중계
    @MessageMapping("/read")
    public void markAsRead(java.util.Map<String, String> payload, Principal principal) {
        String roomId = payload.get("roomId");
        if (roomId == null)
            return;

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) token.getPrincipal();
        Long userId = userDetails.getId();

        log.info("### WebSocket READ signal: room={}, user={} ###", roomId, userId);

        // 1. DB 업데이트 (비동기)
        chatService.markAsRead(roomId, userId);

        // 2. 상대방에게 알림 (신호만 전달)
        java.util.Map<String, Object> signal = new java.util.HashMap<>();
        signal.put("roomId", roomId);
        signal.put("readerId", userId);
        signal.put("type", "READ_SIGNAL");

        messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, signal);
    }

    // REST API 메시지 전송 (이미지 전용)
    @PostMapping(value = "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatMessageResponse> sendChatMessage(
            @RequestPart("request") ChatMessageRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info("Received REST chat message request: room={}, type={}", request.getRoomId(), request.getType());
        return ResponseEntity.ok(chatService.sendMessage(customUserDetails.getId(), request, image));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Slice<ChatMessage>> getChatHistory(
            @PathVariable String roomId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chatService.getChatHistory(roomId, pageable));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @RequestBody ChatRoomCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(chatService.createChatRoom(request, customUserDetails.getId()));
    }
}
