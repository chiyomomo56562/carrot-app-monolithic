package com.carrot.app.domain.chat.dto;

import com.carrot.app.domain.chat.document.ChatMessage;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatEvent(
		String id,
		String roomId,
		Long senderId,
		Long recipientId,
		String content,
		ChatMessage.MessageType type,
		String imageUrl,
		LocalDateTime createdAt) {
}
