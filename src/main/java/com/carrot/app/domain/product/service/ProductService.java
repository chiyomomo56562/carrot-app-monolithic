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
import com.carrot.app.global.common.DistributedLock;
import com.carrot.app.global.exception.CategoryNotFoundException;
import com.carrot.app.global.exception.ForbidenException;
import com.carrot.app.global.exception.UnauthorizedException;
import com.carrot.app.global.exception.UserNotFoundException;
import com.carrot.app.domain.product.producer.ProductEventProducer;
import org.springframework.data.redis.core.RedisTemplate;
import com.carrot.app.domain.product.repository.ProductRepository;
import com.carrot.app.domain.user.repository.UserRepository;
import com.carrot.app.domain.category.repository.CategoryRepository;
import com.carrot.app.domain.product.dto.ProductEvent;
import com.carrot.app.domain.product.dto.ProductUpdateRequest;
import com.carrot.app.global.common.PagedResponse;
import org.springframework.data.domain.PageImpl;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageService productImageService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final ProductEventProducer productEventProducer;

    // distributed lock을 위해 자신을 프로시로 주입 받아야함
    // 이를 위한 순환 의존성 문제를 해결하기 위해 @Lazy를 사용
    @Lazy
    @Autowired
    private ProductService productService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Long categoryId, Pageable pageable) {
        // cache key 생성
        // product:categoryId:pageNumber:list
        String cacheKey = CacheKey.getKey(CacheKey.PRODUCTS,
                (categoryId != null ? categoryId.toString() : "all") + ":" + pageable.getPageNumber(),
                "list");

        // 1. Cache Hit
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            if (cachedValue instanceof PagedResponse) {
                PagedResponse<ProductResponse> pagedResponse = (PagedResponse<ProductResponse>) cachedValue;
                return new PageImpl<>(pagedResponse.getContent(), pageable, pagedResponse.getTotalElements());
            }
            try {
                return (Page<ProductResponse>) cachedValue;
            } catch (ClassCastException e) {
                log.warn("Cache type mismatch for key {}: {}", cacheKey, e.getMessage());
            }
        }

        // 2. Cache Miss - Call AOP protected method
        return productService.loadProductFromDb(categoryId, pageable, cacheKey);
    }

    @DistributedLock(key = "'products:' + #cacheKey")
    public Page<ProductResponse> loadProductFromDb(Long categoryId, Pageable pageable, String cacheKey) {
        // Double check
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            if (cachedValue instanceof PagedResponse) {
                PagedResponse<ProductResponse> pagedResponse = (PagedResponse<ProductResponse>) cachedValue;
                return new PageImpl<>(pagedResponse.getContent(), pageable, pagedResponse.getTotalElements());
            }
            return (Page<ProductResponse>) cachedValue;
        }

        // DB Query
        Page<ProductResponse> result;
        if (categoryId != null) {
            result = productRepository.findAllByCategoryId(categoryId, pageable)
                    .map(ProductResponse::from);
        } else {
            result = productRepository.findAll(pageable).map(ProductResponse::from);
        }

        // Save to Cache
        redisTemplate.opsForValue().set(cacheKey, PagedResponse.from(result), CacheKey.PRODUCTS_TTL, TimeUnit.SECONDS);
        return result;
    }

    // 상품 등록
    public ProductResponse createProduct(ProductCreateRequest request, String userEmail) {
        // S3에 이미지 업로드
        log.info("### ProductService: 이미지 파일 업로드 시작");
        List<String> imageUrls = new ArrayList<>();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            imageUrls = productImageService.uploadImages(request.getImages());
        }
        log.info("### ProductService: 이미지 파일 업로드 완료");

        // db업로드
        return productService.createProductInsideTransaction(request, userEmail, imageUrls);
    }

    @Transactional
    public ProductResponse createProductInsideTransaction(ProductCreateRequest request, String userEmail,
            List<String> imageUrls) {
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
                .thumbnailUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
                .build();

        for (int i = 0; i < imageUrls.size(); i++) {
            product.addImage(ProductImage.builder()
                    .imageUrl(imageUrls.get(i))
                    .orderIndex(i)
                    .build());
        }

        log.info("### ProductService: Saving product with {} images", product.getImages().size());
        Product savedProduct = productRepository.save(product);
        log.info("### ProductService: Product saved with ID: {}", savedProduct.getId());

        // ES index
        productEventProducer.send(savedProduct.getId(), ProductEvent.EventType.CREATED);

        evictProductCache(savedProduct);

        return ProductResponse.from(savedProduct);
    }

    // 상품 상세 조회 - Cache-Aside with Jitter
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        String cacheKey = CacheKey.getKey(CacheKey.PRODUCT_DETAIL, productId.toString(), "detail");

        // 1. Cache Look Aside
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            // Hit
            return (ProductDetailResponse) cachedValue;
        }

        // 2. Cache Miss - Jitter
        Product product = getProductOrThrow(productId);

        UserProfileResponse sellerProfile = UserProfileResponse.from(product.getSeller());

        ProductDetailResponse response = ProductDetailResponse.from(product, sellerProfile);

        // Calculate TTL with Jitter
        // Base: 600s, Jitter: 0~60s
        long ttl = CacheKey.PRODUCT_DETAIL_TTL + (long) (Math.random() * 60);

        redisTemplate.opsForValue().set(cacheKey, response, ttl, TimeUnit.SECONDS);

        return response;
    }

    // 상품 수정
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, String userEmail) {
        // 1. 사전 준비 (트랜잭션 외부)
        // 신규 파일 업로드
        List<String> newImageUrls = new ArrayList<>();
        if (request.getImages() != null) {
            newImageUrls = productImageService.uploadImages(request.getImages());
        }

        // 2. 트랜잭션 내에서 DB 업데이트
        List<String> urlsToDelete = productService.updateProductInsideTransaction(productId, request, userEmail,
                newImageUrls);

        // 3. 사후 처리 (트랜잭션 외부)
        // 삭제된 이미지를 S3에서 제거
        productImageService.deleteImagesFromS3(urlsToDelete);

        // opensearch index 발행
        productEventProducer.send(productId, ProductEvent.EventType.UPDATED);

        // 새 정보를 다시 조회하여 반환
        return productService.getProductResponse(productId);
    }

    @Transactional
    public List<String> updateProductInsideTransaction(Long productId, ProductUpdateRequest request, String userEmail,
            List<String> newImageUrls) {
        Product product = getProductOrThrow(productId);
        validateSeller(product, userEmail);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.getCategoryId()));

        // 기존 이미지 URL 목록 (삭제 대상 식별용)
        List<String> oldUrls = product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

        // 이미지 동기화
        updateImagesInternal(product, request.getKeptImageIds(), newImageUrls);

        // 썸네일 업데이트 (이미지 목록의 첫 번째 이미지를 썸네일로 사용)
        String newThumbnailUrl = product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl();
        product.updateThumbnailUrl(newThumbnailUrl);

        product.update(request.getTitle(), request.getDescription(), request.getPrice().intValue(),
                request.getLocation(), category, request.getStatus());

        log.info("### Updated product status: {}", product.getStatus());

        // opensearch index 발행
        productEventProducer.send(product.getId(), ProductEvent.EventType.UPDATED);

        evictProductCache(product);

        // 결과적으로 삭제된 URL들 반환 (S3 정리를 위해)
        Set<String> currentUrls = product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return oldUrls.stream()
                .filter(url -> !currentUrls.contains(url))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductResponse(Long productId) {
        return ProductResponse.from(getProductOrThrow(productId));
    }

    private void updateImagesInternal(Product product, List<Long> keptImageIdsFromRequest, List<String> newImageUrls) {
        List<Long> keptImageIds = keptImageIdsFromRequest != null ? keptImageIdsFromRequest : new ArrayList<>();

        // 1. 기존 이미지 매핑 (ID -> Entity)
        Map<Long, ProductImage> currentImageMap = product.getImages().stream()
                .collect(Collectors.toMap(ProductImage::getId, img -> img));

        // 2. 소유권 검증 및 유지할 이미지 순서 정렬
        List<ProductImage> synchronizedImages = new ArrayList<>();
        int orderIndex = 0;

        for (Long id : keptImageIds) {
            ProductImage img = currentImageMap.get(id);
            if (img == null) {
                throw new IllegalArgumentException("Invalid image ID: " + id);
            }
            img.updateOrder(orderIndex++);
            synchronizedImages.add(img);
        }

        // 3. 신규 업로드된 이미지 추가
        for (String url : newImageUrls) {
            synchronizedImages.add(ProductImage.builder()
                    .imageUrl(url)
                    .orderIndex(orderIndex++)
                    .product(product)
                    .build());
        }

        // 4. 최대 개수 검증 (예: 10개)
        if (synchronizedImages.size() > 10) {
            throw new IllegalArgumentException("Maximum 10 images allowed");
        }

        // 5. 엔티티의 컬렉션 동기화 호출
        product.updateImages(synchronizedImages);
    }

    // 상품 삭제
    @Transactional
    public void deleteProduct(Long productId, String userEmail) {
        Product product = getProductOrThrow(productId);
        validateSeller(product, userEmail);

        productRepository.delete(product);
        productEventProducer.send(productId, ProductEvent.EventType.DELETED);

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
