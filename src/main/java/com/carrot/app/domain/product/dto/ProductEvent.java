package com.carrot.app.domain.product.dto;

import java.time.LocalDateTime;

public record ProductEvent(
        Long productId,
        EventType eventType,
        LocalDateTime createdAt) {
    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}
