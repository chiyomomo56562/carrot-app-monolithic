package com.carrot.app.domain.search.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.carrot.app.domain.product.event.ProductEvent;
import com.carrot.app.domain.search.service.ProductSearchService;
import com.carrot.app.global.event.AbstractEventConsumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexingConsumer extends AbstractEventConsumer<ProductEvent> {

    private final ProductSearchService productSearchService;

    @KafkaListener(topics = "${spring.kafka.properties.topic.product-event}", groupId = "search-group")
    public void consume(ProductEvent event) {
        handle(event);
    }

    @Override
    protected void processEvent(ProductEvent event) {
        switch (event.eventType()) {
            case PRODUCT_CREATED:
            case PRODUCT_UPDATED:
                productSearchService.indexProduct(event.productId());
                log.info("### Product indexed successfully: {}", event.productId());
                break;
            case PRODUCT_DELETED:
                productSearchService.deleteProduct(event.productId());
                log.info("### Product deleted from index: {}", event.productId());
                break;
            default:
                log.warn("Unknown event type: {}", event.eventType());
        }
    }
}
