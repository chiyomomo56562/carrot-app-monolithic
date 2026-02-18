package com.carrot.app.domain.chat.dto;

import lombok.Builder;

@Builder
public record ChatRoomCreateRequest(Long productId) {
}
