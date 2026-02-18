package com.carrot.app.domain.product.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.carrot.app.domain.product.event.ProductEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.properties.topic.product-event}")
    private String topicName;

    public void send(ProductEvent event) {
        log.info("### Sending product event: {}", event);

        String key = String.valueOf(event.productId());
        kafkaTemplate.send(topicName, key, event);
    }
}
