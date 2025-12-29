package com.carrot.app.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.carrot.app.domain.user.dto.UserEmailCheckResponse;
import com.carrot.app.domain.user.dto.UserNicknameCheckResponse;
import com.carrot.app.domain.user.dto.UserSignUpRequest;
import com.carrot.app.domain.user.dto.UserSignUpResponse;
import com.carrot.app.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("회원가입 성공")
        @WithMockUser
        void signUp_Success() throws Exception {
                // given
                UserSignUpRequest request = UserSignUpRequest.builder()
                                .email("test@example.com")
                                .password("password1234!@#$")
                                .nickname("tester")
                                .location("Seoul")
                                .build();

                MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
                MockMultipartFile imagePart = new MockMultipartFile("profileImage", "image.jpg", "image/jpeg",
                                "image data".getBytes());

                UserSignUpResponse response = UserSignUpResponse.builder()
                                .userId(1L)
                                .email("test@example.com")
                                .nickname("tester")
                                .build();

                given(userService.signUp(any(), any())).willReturn(response);

                // when & then
                mockMvc.perform(multipart("/api/users/signup")
                                .file(requestPart)
                                .file(imagePart)
                                .with(csrf()) // CSRF token required for POST
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(1L))
                                .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @DisplayName("회원가입 실패 - 유효성 검증 실패 (이메일 형식이 아님)")
        @WithMockUser
        void signUp_Fail_InvalidInput() throws Exception {
                // given
                UserSignUpRequest request = UserSignUpRequest.builder()
                                .email("invalid-email") // Not an email
                                .password("password")
                                .nickname("tester")
                                .build();

                MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

                // when & then
                mockMvc.perform(multipart("/api/users/signup")
                                .file(requestPart)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이메일 중복 체크 성공 - JSON 구조 검증")
        @WithMockUser
        void checkEmail_Success() throws Exception {
                // given
                given(userService.checkEmailDuplicate(anyString()))
                                .willReturn(new UserEmailCheckResponse(true)); // duplicate

                // when & then
                mockMvc.perform(get("/api/users/email-check")
                                .param("email", "duplicate@example.com"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.duplicate").value(true));

                // given - available
                given(userService.checkEmailDuplicate(anyString()))
                                .willReturn(new UserEmailCheckResponse(false)); // not duplicate

                // when & then
                mockMvc.perform(get("/api/users/email-check")
                                .param("email", "new@example.com"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.duplicate").value(false));
        }

        @Test
        @DisplayName("닉네임 중복 체크 성공")
        @WithMockUser
        void checkNickname_Success() throws Exception {
                // given
                given(userService.checkNicknameDuplicate(anyString()))
                                .willReturn(new UserNicknameCheckResponse(true));

                // when & then
                mockMvc.perform(get("/api/users/nickname-check")
                                .param("nickname", "duplicateNick"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.duplicate").value(true));
        }

        @Test
        @DisplayName("이메일 인증 성공")
        @WithMockUser
        void verifyEmail_Success() throws Exception {
                // when & then
                mockMvc.perform(get("/api/users/email-verify")
                                .param("token", "valid-token"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().string("Email verified successfully"));
        }
}
