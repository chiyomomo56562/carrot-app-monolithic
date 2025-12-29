package com.carrot.app.infra.email;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import com.carrot.app.domain.user.entity.User;
import com.carrot.app.global.exception.EmailSendException;
import com.carrot.app.global.exception.UserNotFoundException;
import com.carrot.app.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String to, String token) {
        String verificationLink = baseUrl + "/api/users/email-verify?token=" + token;

        try {
            log.info("이메일 발송을 시도합니다. to: {}, token: {}", to, token);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[당근마켓] 회원가입 인증을 완료해주세요.");

            // HTML 형식으로 메일 본문 작성
            String htmlContent = "<h1>안녕하세요!</h1>" +
                    "<p>가입을 축하드립니다. 아래 링크를 클릭하여 인증을 완료해주세요.</p>" +
                    "<a href='" + verificationLink + "'>이메일 인증하기</a>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("이메일 발송을 완료했습니다. to: {}, token: {}", to, token);
        } catch (MessagingException e) {
            log.error("이메일 발송에 실패했습니다.", e);
            throw new EmailSendException("이메일 발송에 실패했습니다.");
        }
    }

    public void sendNotificationEmail(Long recipientId, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            User user = userRepository.findById(recipientId)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

            helper.setTo(user.getEmail());
            helper.setSubject("[당근마켓] 새로운 메시지 알림");

            // HTML 형식으로 메일 본문 작성
            String htmlContent = "<h1>안녕하세요!</h1>" +
                    "<p>새로운 메시지가 도착했습니다.</p>" +
                    "<p>내용: " + content + "</p>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            // 예외 처리 로직 (로그 기록 등)
            throw new EmailSendException("이메일 발송에 실패했습니다.");
        }
    }
}
