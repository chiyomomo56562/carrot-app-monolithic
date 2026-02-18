package com.carrot.app.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.carrot.app.domain.category.entity.Category;
import com.carrot.app.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "products", indexes = {
        // 1. 카테고리별 목록 화면: 특정 카테고리의 상품을 최신순으로 조회할 때 최적화
        @Index(name = "idx_product_category_created", columnList = "category_id, created_at DESC"),
        // 2. 상태별 목록 화면: 특정 상태의 상품을 최신순으로 조회할 때 최적화
        @Index(name = "idx_product_status_created", columnList = "status, created_at DESC")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    public void addImage(ProductImage image) {
        this.images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        this.images.remove(image);
        image.setProduct(null);
    }

    public void setImages(List<ProductImage> imageList) {
        this.images = imageList;
    }

    @Builder.Default
    @Column(name = "thumbnail_url", length = 500, nullable = true)
    private String thumbnailUrl = null;

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, length = 100)
    private String location;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(String title, String description, Integer price, String location, Category category,
            Status status) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.location = location;
        this.category = category;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(Status status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        ON_SALE, RESERVED, SOLD
    }
}
