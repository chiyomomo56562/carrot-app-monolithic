package com.carrot.app.domain.chat.dto;

import java.time.LocalDateTime;

import com.carrot.app.domain.chat.document.ChatRoom;
import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.user.entity.User;

import lombok.Builder;

@Builder
public record ChatRoomResponse(
        String roomId,
        Long productId,
        Long sellerId,
        String sellerNickname,
        String sellerProfileImage,
        Long buyerId,
        String buyerNickname,
        String buyerProfileImage,
        String lastMessage,
        LocalDateTime lastMessageSentAt,
        String productTitle,
        Integer productPrice,
        String productThumbnail,
        Long unreadCount) {

    public static ChatRoomResponse from(ChatRoom room, User seller, User buyer, Product product, Long unreadCount) {
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
    }
}
