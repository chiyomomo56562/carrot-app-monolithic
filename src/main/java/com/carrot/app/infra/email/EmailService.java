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
        log.info("회원가입 인증 이메일 발송을 시도합니다. to: {}", to);
        EmailTemplate template = new VerificationEmailTemplate(baseUrl, token);
        sendEmail(to, template);
    }

    public void sendNotificationEmail(Long recipientId, String content) {
        User user = userRepository.findById(recipientId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        log.info("알림 이메일 발송을 시도합니다. to: {}", user.getEmail());
        EmailTemplate template = new NotificationEmailTemplate(content);
        sendEmail(user.getEmail(), template);
    }

    /**
     * 공통 이메일 발송 로직
     */
    private void sendEmail(String to, EmailTemplate template) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(template.getSubject());
            helper.setText(template.getBody(), true);

            mailSender.send(message);
            log.info("이메일 발송을 완료했습니다. to: {}", to);
        } catch (MessagingException e) {
            log.error("이메일 발송에 실패했습니다. to: {}", to, e);
            throw new EmailSendException("이메일 발송에 실패했습니다.");
        }
    }
}
