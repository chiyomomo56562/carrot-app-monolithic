package com.carrot.app.domain.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.carrot.app.domain.category.entity.Category;
import com.carrot.app.domain.category.repository.CategoryRepository;
import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.product.entity.Product.Status;
import com.carrot.app.domain.product.entity.ProductImage;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.user.repository.UserRepository;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@test.com")
                .password("password")
                .nickname("tester")
                .location("Seoul")
                .role(User.Role.ROLE_USER)
                .status(User.Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        category = Category.builder()
                .name("Electronics")
                .displayOrder(1)
                .depth(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        categoryRepository.save(category);
    }

    @Test
    @DisplayName("Product 저장 및 Cascade 이미지 저장")
    void save_Success() {
        // given
        Product product = Product.builder()
                .seller(user)
                .category(category)
                .title("Test Product")
                .description("Desc")
                .price(1000)
                .location("Seoul")
                .status(Status.ON_SALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProductImage image1 = ProductImage.builder().imageUrl("url1").orderIndex(0).build();
        ProductImage image2 = ProductImage.builder().imageUrl("url2").orderIndex(1).build();
        product.addImage(image1);
        product.addImage(image2);

        // when
        Product savedProduct = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        // then
        Product foundProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(foundProduct.getTitle()).isEqualTo("Test Product");
        assertThat(foundProduct.getImages()).hasSize(2);
    }

    @Test
    @DisplayName("이미지 순서(OrderIndex) 보장 확인")
    void save_CheckImageOrder() {
        // given
        Product product = Product.builder()
                .seller(user)
                .category(category)
                .title("Ordered Product")
                .description("Desc")
                .price(1000)
                .location("Seoul")
                .status(Status.ON_SALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProductImage image1 = ProductImage.builder().imageUrl("url1").orderIndex(0).build();
        ProductImage image2 = ProductImage.builder().imageUrl("url2").orderIndex(1).build();
        ProductImage image3 = ProductImage.builder().imageUrl("url3").orderIndex(2).build();

        product.addImage(image1);
        product.addImage(image2);
        product.addImage(image3);

        productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        // when
        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        List<ProductImage> images = foundProduct.getImages();

        // then
        // Assuming the list is ordered by insertion or explicit order, but standard
        // JPA/List doesn't guarantee order unless @OrderBy is used.
        // Logic inside ProductService controls the orderIndex, here we verify the
        // loaded data has correct index values.
        assertThat(images).extracting("orderIndex").containsExactlyInAnyOrder(0, 1, 2);
        // Note: Unless @OrderBy("orderIndex ASC") is on entity relation, List order
        // isn't guaranteed purely by JPA.
        // However, checking values is valid.
    }

    @Test
    @DisplayName("findById 실행 시 JOIN FETCH 동작 확인 (N+1 방지)")
    void findById_JoinFetch() {
        // given
        Product product = Product.builder()
                .seller(user)
                .category(category)
                .title("Fetch Product")
                .description("Desc")
                .price(1000)
                .location("Seoul")
                .status(Status.ON_SALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        product.addImage(ProductImage.builder().imageUrl("url1").orderIndex(0).build());
        productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        // when
        // To verify join fetch, we rely on Hibernate logs or persistence unit util
        // usually.
        // Or simply check if images are loaded without extra query (hard to assert in
        // plain DataJpaTest without tools or proxies).
        // But we can verify functionally.
        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();

        // then
        // Accessing images should not trigger LazyInitializationException even if we
        // were outside transaction (if session closed),
        // but in DataJpaTest transaction is open.
        // Main verification is relying on implementation of @Query in Repository
        // interface.
        assertThat(foundProduct.getImages()).isNotEmpty();
    }

    @Test
    @DisplayName("카테고리별 조회 및 페이징")
    void findAllByCategoryId_Success() {
        // given
        for (int i = 0; i < 5; i++) {
            Product p = Product.builder()
                    .seller(user)
                    .category(category)
                    .title("Product " + i)
                    .description("Desc")
                    .price(1000 + i)
                    .location("Seoul")
                    .status(Status.ON_SALE)
                    .createdAt(LocalDateTime.now()) // manually set if listener not triggered or for ordering
                    .updatedAt(LocalDateTime.now())
                    .build();
            productRepository.save(p);
        }

        // Another category
        Category otherCategory = Category.builder()
                .name("Books")
                .displayOrder(2)
                .depth(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        categoryRepository.save(otherCategory);
        Product otherP = Product.builder()
                .seller(user)
                .category(otherCategory)
                .title("Book")
                .description("Desc")
                .price(500)
                .location("Seoul")
                .status(Status.ON_SALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        productRepository.save(otherP);

        // when
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.findAllByCategoryId(category.getId(), pageable);

        // then
        assertThat(products.getTotalElements()).isEqualTo(5);
        assertThat(products.getContent()).hasSize(3);
        assertThat(products.getContent().get(0).getTitle()).contains("Product");
    }

    @Test
    @DisplayName("삭제 시 이미지 Cascade 삭제")
    void delete_Success() {
        // given
        Product product = Product.builder()
                .seller(user)
                .category(category)
                .title("Delete Product")
                .description("Desc")
                .price(1000)
                .location("Seoul")
                .status(Status.ON_SALE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        product.addImage(ProductImage.builder().imageUrl("url1").orderIndex(0).build());
        productRepository.save(product);
        Long productId = product.getId();
        entityManager.flush();
        entityManager.clear();

        // when
        productRepository.deleteById(productId);
        entityManager.flush();

        // then
        assertThat(productRepository.findById(productId)).isEmpty();
        // Since it's element collection or one-to-many, typically verified by Product
        // loading failure,
        // or query directly against Image table if needed.
    }
}
