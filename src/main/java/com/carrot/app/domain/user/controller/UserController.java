package com.carrot.app.domain.user.controller;

import com.carrot.app.domain.user.dto.UserEmailCheckRequest;
import com.carrot.app.domain.user.dto.UserEmailCheckResponse;
import com.carrot.app.domain.user.dto.UserNicknameCheckRequest;
import com.carrot.app.domain.user.dto.UserNicknameCheckResponse;
import com.carrot.app.domain.user.dto.UserSignUpRequest;
import com.carrot.app.domain.user.dto.UserSignUpResponse;
import com.carrot.app.domain.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("permitAll()")
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<UserSignUpResponse> signUp(
            @Valid @ModelAttribute UserSignUpRequest request) {
        log.info("request: {}", request);
        UserSignUpResponse response = userService.signUp(request);
        log.info("response: {}", response);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/email-check")
    @ResponseBody
    public ResponseEntity<UserEmailCheckResponse> checkEmail(@RequestParam(name = "email") String email) {
        UserEmailCheckResponse response = userService.checkEmailDuplicate(email);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/nickname-check")
    @ResponseBody
    public ResponseEntity<UserNicknameCheckResponse> checkNickname(
            @RequestParam(name = "nickname") String nickname) {
        return ResponseEntity.ok(userService.checkNicknameDuplicate(nickname));
    }

    // 이메일 인증
    @PreAuthorize("permitAll()")
    @GetMapping("/email-verify")
    public ResponseEntity<String> verifyEmail(@RequestParam(name = "token") String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully");
    }
}
