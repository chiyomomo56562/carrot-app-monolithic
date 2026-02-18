package com.carrot.app.domain.chat.dto;

import com.carrot.app.domain.chat.document.ChatMessage;

import org.springframework.web.multipart.MultipartFile;

import lombok.Builder;

@Builder
public record ChatMessageRequest(
        String roomId,
        String content,
        ChatMessage.MessageType type,
        MultipartFile image) {
}
