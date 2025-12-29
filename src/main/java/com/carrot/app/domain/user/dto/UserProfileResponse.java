package com.carrot.app.domain.user.dto;

import com.carrot.app.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String nickname;
    private String email;
    private String location;
    private String profileImageUrl;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .location(user.getLocation())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
