package com.carrot.app.domain.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.carrot.app.domain.chat.document.ChatMessage;
import com.carrot.app.domain.chat.dto.ChatMessageRequest;
import com.carrot.app.domain.chat.dto.ChatMessageResponse;
import com.carrot.app.domain.chat.dto.ChatRoomCreateRequest;
import com.carrot.app.domain.chat.dto.ChatRoomResponse;
import com.carrot.app.domain.chat.service.ChatService;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ChatService chatService;

        @MockBean
        private org.springframework.messaging.simp.SimpMessageSendingOperations messagingTemplate; // Controller uses
                                                                                                   // this

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("메시지 전송 (REST) 성공")
        void sendChatMessage_Rest_Success() throws Exception {
                // given
                ChatMessageRequest request = new ChatMessageRequest();
                request.setRoomId("room1");
                request.setContent("Hello");

                User mockUser = User.builder().id(2L).email("attacker").password("pw").role(User.Role.ROLE_USER)
                                .status(User.Status.ACTIVE).build();
                CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);

                MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

                ChatMessageResponse response = ChatMessageResponse.builder().id("msg1").roomId("room1").content("Hello")
                                .build();
                given(chatService.sendMessage(anyLong(), any(), any())).willReturn(response);

                // when & then
                mockMvc.perform(multipart("/api/chats/messages")
                                .file(requestPart)
                                .with(csrf())
                                .with(user(customUserDetails))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("msg1"));
        }

        @Test
        @DisplayName("채팅 기록 조회 성공")
        @WithMockUser
        void getChatHistory_Success() throws Exception {
                // given
                String roomId = "room1";
                Slice<ChatMessage> slice = new SliceImpl<>(new ArrayList<>());
                given(chatService.getChatHistory(eq(roomId), any())).willReturn(slice);

                // when & then
                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
                                .param("page", "0")
                                .param("size", "20"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("채팅방 생성 성공")
        void createChatRoom_Success() throws Exception {
                // given
                ChatRoomCreateRequest request = new ChatRoomCreateRequest(10L);
                ChatRoomResponse response = ChatRoomResponse.builder().roomId("room1").build();
                given(chatService.createChatRoom(any(), anyLong())).willReturn(response);

                User mockUser = User.builder().id(2L).email("attacker").password("pw").role(User.Role.ROLE_USER)
                                .status(User.Status.ACTIVE).build();
                CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);

                // when & then
                mockMvc.perform(post("/api/chats/rooms")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(csrf())
                                .with(user(customUserDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.roomId").value("room1"));
        }
}
