package com.carrot.app.domain.chat.dto;

import com.carrot.app.domain.chat.document.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private String roomId;
    private String content;
    private ChatMessage.MessageType type;
}
