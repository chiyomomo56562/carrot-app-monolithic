package com.carrot.app.domain.user.service;

import com.carrot.app.domain.user.dto.UserEmailCheckResponse;
import com.carrot.app.domain.user.dto.UserNicknameCheckResponse;
import com.carrot.app.domain.user.dto.UserProfileResponse;
import com.carrot.app.domain.user.dto.UserSignUpRequest;
import com.carrot.app.domain.user.dto.UserSignUpResponse;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.user.repository.UserRepository;
import com.carrot.app.global.exception.UserNotFoundException;
import com.carrot.app.infra.s3.S3Service;
import com.carrot.app.infra.email.EmailVerifyEvent;
import com.carrot.app.global.exception.EmailAlreadyExistsException;
import com.carrot.app.global.exception.EmailVerifyError;
import com.carrot.app.global.exception.NicknameAlreadyExistsException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final S3Service s3Service;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public UserSignUpResponse signUp(UserSignUpRequest request, MultipartFile profileImage) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("이미 가입된 이메일입니다.");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new NicknameAlreadyExistsException("이미 사용 중인 닉네임입니다.");
        }

        log.info("### profileImage: {}", profileImage);
        // s3 업로드 로직
        String profileImageUrl = s3Service.uploadOptimizedImage(profileImage);

        log.info("profileImageUrl: {}", profileImageUrl);
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .location(request.getLocation())
                .profileImageUrl(profileImageUrl)
                .role(User.Role.ROLE_USER)
                .status(User.Status.PENDING)
                .build();

        User savedUser = userRepository.save(user);

        // 토큰 생성
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(token, user.getEmail(), 24L, TimeUnit.HOURS);

        log.info("### token: {}", token);
        // 유저 회원 가입 시 이메일 전송을 해야함.
        kafkaTemplate.send("email-verify", new EmailVerifyEvent(user.getEmail(), token));

        log.info("### 이메일 발송 이벤트 발급 완료");
        return UserSignUpResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .build();
    }

    public UserEmailCheckResponse checkEmailDuplicate(String email) {
        boolean isDuplicate = userRepository.existsByEmail(email);
        return new UserEmailCheckResponse(isDuplicate);
    }

    public UserNicknameCheckResponse checkNicknameDuplicate(String nickname) {
        boolean isDuplicate = userRepository.existsByNickname(nickname);
        return new UserNicknameCheckResponse(isDuplicate);
    }

    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserProfileResponse.from(user);
    }

    // 이메일 인증
    @Transactional
    public void verifyEmail(String token) {
        String email = redisTemplate.opsForValue().get(token);
        if (email == null) {
            throw new EmailVerifyError("Invalid token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailVerifyError("User not found with email: " + email));
        user.setStatus(User.Status.ACTIVE);
        redisTemplate.delete(token);
    }
}
