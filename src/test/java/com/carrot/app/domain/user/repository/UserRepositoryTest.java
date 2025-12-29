package com.carrot.app.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.carrot.app.domain.user.entity.User;

import java.time.LocalDateTime;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("User 저장 성공")
    void save_Success() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("tester")
                .location("Seoul")
                .role(User.Role.ROLE_USER)
                .status(User.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when
        User savedUser = userRepository.save(user);

        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("User 저장 실패 - 필수 필드 누락")
    void save_Fail_NullField() {
        // given
        User user = User.builder()
                .email(null) // 필수 필드 누락
                .password("password")
                .nickname("tester")
                .location("Seoul")
                .role(User.Role.ROLE_USER)
                .status(User.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> userRepository.save(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Email로 User 조회 성공")
    void findByEmail_Success() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("tester")
                .location("Seoul")
                .role(User.Role.ROLE_USER)
                .status(User.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // when
        User foundUser = userRepository.findByEmail("test@example.com").orElse(null);

        // then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Email 존재 여부 확인")
    void existsByEmail_True_False() {
        // given
        User user = User.builder()
                .email("exist@example.com")
                .password("password")
                .nickname("tester")
                .location("Seoul")
                .role(User.Role.ROLE_USER)
                .status(User.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // when & then
        assertThat(userRepository.existsByEmail("exist@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@example.com")).isFalse();
    }

    @Test
    @DisplayName("Nickname 존재 여부 확인")
    void existsByNickname_True_False() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("existNick")
                .location("Seoul")
                .role(User.Role.ROLE_USER)
                .status(User.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // when & then
        assertThat(userRepository.existsByNickname("existNick")).isTrue();
        assertThat(userRepository.existsByNickname("noneNick")).isFalse();
    }

    @Test
    @DisplayName("User 상태 수정 반영 확인")
    void update_Success() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("tester")
                .location("Seoul")
                .role(User.Role.ROLE_USER)
                .status(User.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        User savedUser = userRepository.save(user);

        // when
        savedUser.setStatus(User.Status.ACTIVE);
        // flush to force DB update for testing
        userRepository.flush();

        User updatedUser = userRepository.findById(savedUser.getId()).get();

        // then
        assertThat(updatedUser.getStatus()).isEqualTo(User.Status.ACTIVE);
    }
}
