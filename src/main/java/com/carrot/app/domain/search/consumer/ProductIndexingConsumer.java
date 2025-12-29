package com.carrot.app.domain.search.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.carrot.app.domain.product.dto.ProductEvent;
import com.carrot.app.domain.search.service.ProductSearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexingConsumer {

    private final ProductSearchService productSearchService;

    @KafkaListener(topics = "${spring.kafka.properties.topic.product-event}", groupId = "search-group")
    public void consume(ProductEvent event) {
        log.info("### Product event consumed: type={}, productId={}", event.eventType(), event.productId());

        switch (event.eventType()) {
            case CREATED:
            case UPDATED:
                productSearchService.indexProduct(event.productId());
                log.info("### Product indexed successfully: {}", event.productId());
                break;
            case DELETED:
                productSearchService.deleteProduct(event.productId());
                log.info("### Product deleted from index: {}", event.productId());
                break;
            default:
                log.warn("Unknown event type: {}", event.eventType());
        }
    }
}
