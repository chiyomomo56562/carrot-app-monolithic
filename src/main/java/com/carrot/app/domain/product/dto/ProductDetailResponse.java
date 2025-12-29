package com.carrot.app.domain.product.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.user.dto.UserProfileResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {
    private Long id;
    private String title;
    private String description;
    private Integer price;
    private Long categoryId;
    private String categoryName;
    private String location;
    private List<ProductImageResponse> images;
    private Integer viewCount;
    private Integer likeCount;
    private Integer chatCount;
    private UserProfileResponse seller;
    private String status;
    private LocalDateTime createdAt;

    public static ProductDetailResponse from(Product product, UserProfileResponse seller) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .location(product.getLocation())
                .images(product.getImages().stream().map(ProductImageResponse::from)
                        .toList())
                .seller(seller)
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
