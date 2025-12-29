package com.carrot.app.domain.chat.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private String roomId;
    private Long productId;
    private Long sellerId;
    private String sellerNickname;
    private String sellerProfileImage;
    private Long buyerId;
    private String buyerNickname;
    private String buyerProfileImage;
    private String lastMessage;
    private LocalDateTime lastMessageSentAt;

    private String productTitle;
    private Integer productPrice;
    private String productThumbnail;
    private Long unreadCount;
}
