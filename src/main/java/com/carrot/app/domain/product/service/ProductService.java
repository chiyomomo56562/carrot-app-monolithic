package com.carrot.app.domain.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrot.app.domain.product.dto.ProductCreateRequest;
import com.carrot.app.domain.product.dto.ProductDetailResponse;
import com.carrot.app.domain.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.category.entity.Category;
import com.carrot.app.domain.user.dto.UserProfileResponse;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.global.common.CacheKey;
import com.carrot.app.global.exception.CategoryNotFoundException;
import com.carrot.app.global.exception.UnauthorizedException;
import com.carrot.app.global.exception.UserNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import com.carrot.app.domain.product.repository.ProductRepository;
import com.carrot.app.domain.user.repository.UserRepository;
import com.carrot.app.domain.category.repository.CategoryRepository;
import com.carrot.app.domain.product.dto.ProductUpdateRequest;
import com.carrot.app.global.common.PagedResponse;
import com.carrot.app.global.event.DomainEventFactory;

import org.springframework.data.domain.PageImpl;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Set;

import com.carrot.app.domain.product.entity.ProductImage;
import com.carrot.app.domain.product.event.ProductEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductQueryService productQueryService;
    private final ProductImageManager productImageManager;

    // private final ProductEventProducer productEventProducer;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Page<ProductResponse> getProducts(Long categoryId, Pageable pageable) {
        return productQueryService.getProducts(categoryId, pageable);
    }

    // 상품 등록
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, String userEmail) {
        // 상품 생성
        User seller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userEmail));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.getCategoryId()));

        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice().intValue())
                .location(request.getLocation())
                .status(Product.Status.ON_SALE)
                .build();

        // S3에 이미지 업로드
        productImageManager.processCreateProductImage(product, request.getImages());

        // DB에 상품 저장
        productRepository.save(product);

        // ES 이벤트 발생
        applicationEventPublisher.publishEvent(DomainEventFactory.productEventCreated(product.getId(),
                ProductEvent.EventType.PRODUCT_CREATED));

        // 캐시 삭제
        evictProductCache(product);

        return ProductResponse.from(product);
    }

    // 상품 조회
    public ProductDetailResponse getProductDetail(Long productId) {
        return productQueryService.getProductDetail(productId);
    }

    // 상품 수정
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, String userEmail) {
        // 1. 상품 정보 수정 (이미지 정보 제외)
        Product product = getProductOrThrow(productId);
        validateSeller(product, userEmail);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.getCategoryId()));

        product.update(request.getTitle(), request.getDescription(), request.getPrice().intValue(),
                request.getLocation(), category, request.getStatus());

        // 2. 이미지 정보 수정
        productImageManager.processUpdateProductImage(product, request.getImageOrder(), request.getNewImages());

        // 3. 캐시 삭제
        evictProductCache(product);

        return getProductResponse(productId);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductResponse(Long productId) {
        return ProductResponse.from(getProductOrThrow(productId));
    }

    // 상품 삭제
    @Transactional
    public void deleteProduct(Long productId, String userEmail) {
        Product product = getProductOrThrow(productId);
        validateSeller(product, userEmail);

        productRepository.delete(product);

        applicationEventPublisher.publishEvent(DomainEventFactory.productEventCreated(productId,
                ProductEvent.EventType.PRODUCT_DELETED));

        evictProductCache(product);
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    private void validateSeller(Product product, String userEmail) {
        if (!product.getSeller().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("Unauthorized: Access denied for " + userEmail);
        }
    }

    private void evictProductCache(Product product) {
        // Detail cache eviction
        String detailCacheKey = CacheKey.getKey(CacheKey.PRODUCT_DETAIL, product.getId().toString(), "detail");
        redisTemplate.delete(detailCacheKey);

        // List cache eviction (First page)
        redisTemplate.delete(CacheKey.getKey(CacheKey.PRODUCTS, "all:0", "list"));
        if (product.getCategory() != null) {
            String categoryListKey = CacheKey.getKey(CacheKey.PRODUCTS, product.getCategory().getId().toString() + ":0",
                    "list");
            redisTemplate.delete(categoryListKey);
        }
    }
}
