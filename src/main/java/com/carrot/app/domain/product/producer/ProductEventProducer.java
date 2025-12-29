package com.carrot.app.domain.product.producer;

import com.carrot.app.domain.product.dto.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.properties.topic.product-event}")
    private String topicName;

    public void send(Long productId, ProductEvent.EventType type) {
        ProductEvent event = new ProductEvent(productId, type, LocalDateTime.now());
        log.info("### Sending product event: {}", event);

        String key = String.valueOf(productId);
        kafkaTemplate.send(topicName, key, event);
    }
}
