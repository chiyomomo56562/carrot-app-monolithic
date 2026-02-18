package com.carrot.app.domain.chat.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "chat_message")
@CompoundIndex(name = "idx_room_created", def = "{'roomId': 1, 'createdAt': -1}")
@CompoundIndex(name = "idx_room_read", def = "{'roomId': 1, 'isRead': 1}")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    @Field("roomId")
    private String roomId;

    @Field("senderId")
    private Long senderId;

    @Field("content")
    private String content;

    @Field("type")
    private MessageType type;

    @Field("isRead")
    @Builder.Default
    private Boolean isRead = false;

    @Field("imageUrl")
    private String imageUrl;

    @CreatedDate
    @Field("createdAt")
    private LocalDateTime createdAt;

    public enum MessageType {
        TEXT, IMAGE
    }
}
