package com.carrot.app.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.multipart.MultipartFile;

import com.carrot.app.domain.category.entity.Category;
import com.carrot.app.domain.category.repository.CategoryRepository;
import com.carrot.app.domain.product.dto.ProductCreateRequest;
import com.carrot.app.domain.product.dto.ProductDetailResponse;
import com.carrot.app.domain.product.dto.ProductEvent;
import com.carrot.app.domain.product.dto.ProductResponse;
import com.carrot.app.domain.product.dto.ProductUpdateRequest;
import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.product.entity.Product.Status;
import com.carrot.app.domain.product.entity.ProductImage;
import com.carrot.app.domain.product.producer.ProductEventProducer;
import com.carrot.app.domain.product.repository.ProductRepository;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.user.repository.UserRepository;
import com.carrot.app.global.common.PagedResponse;
import com.carrot.app.global.exception.UnauthorizedException;
import com.carrot.app.global.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;
    @Mock
    private ProductService selfProxy;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductImageService productImageService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductEventProducer productEventProducer;

    @Test
    @DisplayName("상품 목록 조회 - Cache Hit (캐시에 데이터가 있으면 DB 조회를 하지 않는다)")
    void getProducts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // redisTemplate.opsForValue()가 호출되면 mock 객체인 valueOperations를 반환하도록 설정
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        List<ProductResponse> content = new ArrayList<>();
        // PagedResponse 생성 (필드 개수에 맞춰 생성자 호출)
        PagedResponse<ProductResponse> cachedValue = new PagedResponse<>(
                content, 0, 20, 0L, 1, true);

        // 캐시(Redis)에서 특정 키로 조회했을 때 준비한 cachedValue를 반환하도록 설정
        given(valueOperations.get(anyString())).willReturn(cachedValue);

        // 2. When: 실제 서비스 로직 실행
        Page<ProductResponse> result = productService.getProducts(null, pageable);

        // 3. Then: 결과 검증
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);

        verify(valueOperations, times(0)).set(anyString(), any(), anyLong(), any());
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("상품 등록 성공")
    void createProduct_Success() {
        // given
        ProductCreateRequest request = ProductCreateRequest.builder()
                .title("Title")
                .description("Description")
                .categoryId(1L)
                .price(1000)
                .location("Location")
                .build();

        ProductResponse mockResponse = ProductResponse.builder().id(1L).build();

        User user = User.builder().email("test@test.com").build();

        Category category = Category.builder().id(1L).build();

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(categoryRepository.findById(anyLong())).willReturn(Optional.of(category));

        Product savedProduct = Product.builder()
                .id(1L)
                .seller(user)
                .category(category)
                .images(new ArrayList<>())
                .title("Title")
                .description("Description")
                .price(1000)
                .location("Location")
                .status(Product.Status.ON_SALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        given(productRepository.save(any(Product.class))).willReturn(savedProduct);

        // when
        // Calling directly (bypassing proxy for unit testing logic inside)
        ProductResponse response = productService.createProductInsideTransaction(request, "test@test.com",
                List.of("url1"));

        // then
        assertThat(response.getId()).isEqualTo(1L);
        verify(productEventProducer).send(eq(1L), eq(ProductEvent.EventType.CREATED));
    }

    @Test
    @DisplayName("상품 등록 실패 - 트랜잭션 롤백 (S3 성공 후 DB 실패)")
    void createProduct_Fail_TransactionRollback() {
        // DB save mock to throw exception
        ProductCreateRequest request = ProductCreateRequest.builder()
                .title("Title")
                .description("Description")
                .categoryId(1L)
                .price(1000)
                .location("Location")
                .build();

        User user = User.builder().email("test@test.com").build();
        Category category = Category.builder().id(1L).build();

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(categoryRepository.findById(anyLong())).willReturn(Optional.of(category));
        given(productRepository.save(any(Product.class))).willThrow(new RuntimeException("DB Error"));

        // when & then
        assertThatThrownBy(
                () -> productService.createProductInsideTransaction(request, "test@test.com", List.of("url1")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("상품 상세 조회 성공 - DB Fetch & Jitter")
    void getProductDetail_Success() {
        // given
        Long productId = 1L;
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null); // Cache miss

        User seller = User.builder().email("test@test.com").nickname("Seller").build();
        Category category = Category.builder().name("Cat").build();
        Product product = Product.builder()
                .id(productId)
                .seller(seller)
                .category(category)
                .title("Title")
                .description("Description")
                .price(1000)
                .location("Location")
                .status(Product.Status.RESERVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        ProductDetailResponse response = productService.getProductDetail(productId);

        // then
        assertThat(response.getId()).isEqualTo(productId);
        verify(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("상품 수정 성공 - 이미지 동기화 로직 및 S3 삭제 확인")
    void updateProduct_Success() {
        // given
        Long productId = 1L;
        String userEmail = "test@test.com";
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setCategoryId(1L);
        request.setTitle("New Title");
        request.setPrice(2000);
        request.setStatus(Status.ON_SALE);
        request.setKeptImageIds(List.of(100L)); // Keep image with ID 100

        List<String> newImageUrls = List.of("newUrl1");

        User seller = User.builder().email(userEmail).build();
        Category category = Category.builder().id(1L).build();

        ProductImage keepImage = ProductImage.builder().id(100L).imageUrl("oldUrl1").build();
        ProductImage deleteImage = ProductImage.builder().id(200L).imageUrl("oldUrl2").build();

        Product product = Product.builder()
                .id(productId)
                .seller(seller)
                .category(category)
                .images(new ArrayList<>(Arrays.asList(keepImage, deleteImage)))
                .build();
        keepImage.setProduct(product);
        deleteImage.setProduct(product);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(categoryRepository.findById(anyLong())).willReturn(Optional.of(category));

        // when
        List<String> deletedUrls = productService.updateProductInsideTransaction(productId, request, userEmail,
                newImageUrls);

        // then
        assertThat(deletedUrls).containsExactly("oldUrl2"); // 'oldUrl2' should be deleted
        assertThat(product.getTitle()).isEqualTo("New Title");
        assertThat(product.getImages()).hasSize(2); // 1 kept + 1 new
        assertThat(product.getImages()).extracting("imageUrl").contains("oldUrl1", "newUrl1");

        verify(productEventProducer).send(eq(productId), eq(ProductEvent.EventType.UPDATED));
    }

    @Test
    @DisplayName("상품 수정 실패 - 권한 없음")
    void updateProduct_Fail_Unauthorized() {
        // given
        Long productId = 1L;
        String ownerEmail = "owner@test.com";
        String attackerEmail = "attacker@test.com";

        Product product = Product.builder()
                .id(productId)
                .seller(User.builder().email(ownerEmail).build())
                .build();

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.updateProductInsideTransaction(productId, new ProductUpdateRequest(),
                attackerEmail, null))
                .isInstanceOf(UnauthorizedException.class);
    }
}
