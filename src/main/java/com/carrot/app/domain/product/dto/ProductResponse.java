package com.carrot.app.domain.product.dto;

import java.time.LocalDateTime;

import com.carrot.app.domain.product.entity.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String thumbnailUrl;
    private String title;
    private Integer price;
    private String status;
    private String location;
    private LocalDateTime createdAt;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .thumbnailUrl(product.getThumbnailUrl())
                .title(product.getTitle())
                .price(product.getPrice())
                .status(product.getStatus().name())
                .location(product.getLocation())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
