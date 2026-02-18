package com.carrot.app.infra.email.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.carrot.app.infra.email.EmailService;
import com.carrot.app.infra.email.EmailVerifyEvent;
import com.carrot.app.global.event.AbstractEventConsumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventConsumer extends AbstractEventConsumer<EmailVerifyEvent> {

    private final EmailService emailService;

    @KafkaListener(topics = "user.email.verify", groupId = "email-group")
    public void consume(EmailVerifyEvent event) {
        handle(event);
    }

    @Override
    protected void processEvent(EmailVerifyEvent event) {
        emailService.sendVerificationEmail(event.email(), event.token().toString());
        log.info("### Email sent successfully to: {}", event.email());
    }
}
