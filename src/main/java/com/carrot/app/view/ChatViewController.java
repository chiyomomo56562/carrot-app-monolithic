package com.carrot.app.view;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.carrot.app.domain.chat.dto.ChatRoomResponse;
import com.carrot.app.domain.chat.service.ChatService;
import com.carrot.app.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatViewController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public String getChatRooms(
            Model model,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(size = 20, sort = "lastMessageSentAt", direction = Sort.Direction.DESC) Pageable pageable) {
        model.addAttribute("chatRooms", chatService.getChatRooms(customUserDetails.getId(), pageable));
        model.addAttribute("currentUserId", customUserDetails.getId());
        return "chats/room-list";
    }

    @GetMapping("/rooms/{roomId}")
    public String chatRoomPage(@PathVariable String roomId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Model model) {
        model.addAttribute("room", chatService.getChatRoom(roomId));
        model.addAttribute("currentUserId", customUserDetails.getId());
        // CSRF 설정을 위해 X-XSRF-TOKEN 처리가 필요한 페이지
        return "chats/room-detail";
    }
}