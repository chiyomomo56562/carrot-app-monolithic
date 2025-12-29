package com.carrot.app.view;

import com.carrot.app.domain.user.service.UserService;
import com.carrot.app.global.security.CustomUserDetails;

import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserViewController {
    private final UserService userService;

    @GetMapping("/signup")
    public String signupPage() {
        return "users/signup";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "users/login";
    }

    @GetMapping("/me")
    public String myProfile(
            Model model,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        model.addAttribute("user", userService.getUserProfile(customUserDetails.getId()));
        return "users/my-profile";
    }
}