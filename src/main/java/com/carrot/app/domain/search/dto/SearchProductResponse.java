package com.carrot.app.domain.search.dto;

import java.time.LocalDateTime;

import com.carrot.app.domain.search.document.ProductDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchProductResponse {
    private Long id;
    private String title;
    private String description;
    private Integer price;
    private String location;
    private String thumbnailUrl;
    private String status;
    private String categoryName;
    private LocalDateTime createdAt;
    private String sellerNickname;

    public static SearchProductResponse from(ProductDocument document) {
        return SearchProductResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .price(document.getPrice())
                .location(document.getLocation())
                .thumbnailUrl(document.getThumbnailUrl())
                .status(document.getStatus())
                .categoryName(document.getCategoryName())
                .createdAt(document.getCreatedAt())
                .sellerNickname(document.getSellerNickname())
                .build();
    }
}
