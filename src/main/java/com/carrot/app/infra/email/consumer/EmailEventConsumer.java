package com.carrot.app.infra.email.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.carrot.app.infra.email.EmailService;
import com.carrot.app.infra.email.EmailVerifyEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@Component
@RequiredArgsConstructor
public class EmailEventConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "email-verify", groupId = "email-group")
    public void consume(EmailVerifyEvent event) {
        log.info("### Email verification event consumed: email={}", event.email());

        emailService.sendVerificationEmail(event.email(), event.token().toString());

        log.info("### Email sent successfully to: {}", event.email());
    }
}
