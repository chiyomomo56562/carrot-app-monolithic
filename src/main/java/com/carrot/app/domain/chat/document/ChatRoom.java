package com.carrot.app.domain.chat.document;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "chat_room")
@CompoundIndex(name = "idx_room_created", def = "{'sellerId': 1, 'lastMessageSentAt': -1}")
@CompoundIndex(name = "idx_room_created", def = "{'buyerId': 1, 'lastMessageSentAt': -1}")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    private String id;

    @Indexed(name = "idx_chatroom_product")
    @Field("productId")
    private Long productId;

    @Field("sellerId")
    private Long sellerId;

    @Field("buyerId")
    private Long buyerId;

    @Field("lastMessage")
    private String lastMessage;

    @Indexed(name = "idx_chatroom_last_sent", direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
    @Field("lastMessageSentAt")
    private LocalDateTime lastMessageSentAt;

    @CreatedDate
    @Field("createdAt")
    private LocalDateTime createdAt;
}
