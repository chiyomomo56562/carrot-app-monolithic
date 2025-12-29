package com.carrot.app.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import com.carrot.app.domain.user.dto.UserSignUpRequest;
import com.carrot.app.domain.user.dto.UserSignUpResponse;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.user.repository.UserRepository;
import com.carrot.app.global.exception.EmailAlreadyExistsException;
import com.carrot.app.global.exception.EmailVerifyError;
import com.carrot.app.global.exception.NicknameAlreadyExistsException;
import com.carrot.app.infra.s3.S3Service;
import com.carrot.app.infra.email.EmailVerifyEvent;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private S3Service s3Service;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MultipartFile profileImage;

    @Test
    @DisplayName("회원가입 성공 - Password Encoding 검증 포함")
    void signUp_Success() {
        // given
        UserSignUpRequest request = UserSignUpRequest.builder()
                .email("test@example.com")
                .password("password")
                .nickname("tester")
                .location("Seoul")
                .build();

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(s3Service.uploadOptimizedImage(any())).willReturn("image-url");
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

        User savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("tester")
                .build();
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        UserSignUpResponse response = userService.signUp(request, profileImage);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        // Verify Password Encoding
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPassword()).isEqualTo("encodedPassword");

        // Verify interactions
        verify(kafkaTemplate).send(anyString(), any(EmailVerifyEvent.class));
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_Fail_EmailDuplicate() {
        // given
        UserSignUpRequest request = UserSignUpRequest.builder()
                .email("duplicate@example.com")
                .build();
        given(userRepository.existsByEmail(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUp(request, profileImage))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signUp_Fail_NicknameDuplicate() {
        // given
        UserSignUpRequest request = UserSignUpRequest.builder()
                .email("test@example.com")
                .nickname("duplicateNick")
                .build();
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUp(request, profileImage))
                .isInstanceOf(NicknameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("회원가입 실패 - S3 업로드 에러 시 예외 전파")
    void signUp_Fail_S3UploadError() {
        // given
        UserSignUpRequest request = UserSignUpRequest.builder()
                .email("test@example.com")
                .password("password")
                .nickname("tester")
                .build();

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        doThrow(new RuntimeException("S3 Error")).when(s3Service).uploadOptimizedImage(any());

        // when & then
        assertThatThrownBy(() -> userService.signUp(request, profileImage))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 Error");

        // Transaction rollback would happen here in integration test,
        // in unit test we check code execution stops before save
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("이메일 인증 성공")
    void verifyEmail_Success() {
        // given
        String token = "valid-token";
        String email = "test@example.com";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(token)).willReturn(email);

        User user = User.builder().email(email).status(User.Status.PENDING).build();
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        userService.verifyEmail(token);

        // then
        assertThat(user.getStatus()).isEqualTo(User.Status.ACTIVE);
        verify(redisTemplate).delete(token);
    }

    @Test
    @DisplayName("이메일 인증 실패 - 유효하지 않은 토큰 (Redis 누락/만료)")
    void verifyEmail_Fail_InvalidToken() {
        // given
        String token = "invalid-token";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(token)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> userService.verifyEmail(token))
                .isInstanceOf(EmailVerifyError.class)
                .hasMessage("Invalid token");
    }

    @Test
    @DisplayName("이메일 인증 실패 - 유저 없음")
    void verifyEmail_Fail_UserNotFound() {
        // given
        String token = "valid-token";
        String email = "test@example.com";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(token)).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.verifyEmail(token))
                .isInstanceOf(EmailVerifyError.class)
                .hasMessageContaining("User not found");
    }
}
