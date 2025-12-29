package com.carrot.app.domain.user.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.core.annotation.Order;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class UserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("이미 사용자가 존재하여 초기 데이터를 생성하지 않습니다.");
            return;
        }

        log.info("초기 유저 데이터 생성을 시작합니다.");

        // 유저 1
        User user1 = User.builder()
                .email("asdf1234@asdf.com")
                .password(passwordEncoder.encode("qwer1234!@#$"))
                .nickname("치요모모")
                .location("서울시 강남구")
                .profileImageUrl("null")
                .role(User.Role.ROLE_USER)
                .status(User.Status.ACTIVE) // ACTIVE 상태
                .build();
        // 유저 2
        User user2 = User.builder()
                .email("qwer1234@qwer.com")
                .password(passwordEncoder.encode("qwer1234!@#$"))
                .nickname("당근왕자")
                .location("경기도 성남시")
                .profileImageUrl("null")
                .role(User.Role.ROLE_USER)
                .status(User.Status.ACTIVE) // ACTIVE 상태
                .build();

        // 유저 3 (관리자 예시)
        User user3 = User.builder()
                .email("admin@example.com")
                .password(passwordEncoder.encode("qwer1234!@#$"))
                .nickname("운영자")
                .location("서울특별시")
                .profileImageUrl("null")
                .role(User.Role.ROLE_ADMIN)
                .status(User.Status.ACTIVE) // ACTIVE 상태
                .build();

        userRepository.saveAll(List.of(user1, user2, user3));
        log.info("유저 3명 생성 완료 (상태: ACTIVE)");
    }
}