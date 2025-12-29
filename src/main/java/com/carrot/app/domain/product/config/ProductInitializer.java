package com.carrot.app.domain.product.config;

import com.carrot.app.domain.category.entity.Category;
import com.carrot.app.domain.category.repository.CategoryRepository;
import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.product.entity.ProductImage;
import com.carrot.app.domain.product.repository.ProductRepository;
import com.carrot.app.domain.user.entity.User;
import com.carrot.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class ProductInitializer implements CommandLineRunner {

        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;
        private final UserRepository userRepository;

        @Value("${aws.cloudfront.domain}")
        private String cloudfrontDomain;

        @Override
        @Transactional
        public void run(String... args) {
                if (productRepository.count() > 0) {
                        log.info("Products already exist. Skipping initialization.");
                        return;
                }

                log.info("Initializing products...");

                User user1 = userRepository.findByEmail("asdf1234@asdf.com")
                                .orElseThrow(() -> new RuntimeException("Seed user 1 not found"));
                User user2 = userRepository.findByEmail("qwer1234@qwer.com")
                                .orElseThrow(() -> new RuntimeException("Seed user 2 not found"));

                Category digital = categoryRepository.findByName("디지털기기")
                                .orElseThrow(() -> new RuntimeException("Category '디지털기기' not found"));
                Category app = categoryRepository.findByName("생활가전")
                                .orElseThrow(() -> new RuntimeException("Category '생활가전' not found"));
                Category clothing = categoryRepository.findByName("의류")
                                .orElseThrow(() -> new RuntimeException("Category '의류' not found"));

                Product p1 = Product.builder()
                                .title("아이폰 15 프로 256GB")
                                .description("미개봉 새제품입니다. 선물받았는데 필요없어서 팔아요.")
                                .price(1300000)
                                .category(digital)
                                .seller(user1)
                                .location("서울시 강남구")
                                .thumbnailUrl(cloudfrontDomain
                                                + "product-images/21e66ad8-2501-4601-b4bb-2c7233e1b1e7.jpg")
                                .status(Product.Status.ON_SALE)
                                .build();
                p1.addImage(ProductImage.builder().imageUrl(p1.getThumbnailUrl()).orderIndex(0).build());
                p1.addImage(ProductImage.builder()
                                .imageUrl(cloudfrontDomain + "product-images/d43ec6a2-374d-44fc-b4d7-c4f262061fdd.jpg")
                                .orderIndex(1)
                                .build());

                Product p2 = Product.builder()
                                .title("LG 디오스 냉장고")
                                .description("3년 사용했습니다. 깨끗하게 관리했습니다.")
                                .price(450000)
                                .category(app)
                                .seller(user2)
                                .thumbnailUrl(cloudfrontDomain
                                                + "product-images/435ead7b-fa0d-446f-805d-050a86a299d7.jpg")
                                .location("경기도 성남시")
                                .status(Product.Status.ON_SALE)
                                .build();
                p2.addImage(ProductImage.builder().imageUrl(p2.getThumbnailUrl()).orderIndex(0).build());
                p2.addImage(ProductImage.builder()
                                .imageUrl(cloudfrontDomain + "product-images/e98d2ddc-c32e-4917-9468-f244e06da83f.jpg")
                                .orderIndex(1)
                                .build());

                Product p3 = Product.builder()
                                .title("나이키 에어포스 1")
                                .description("사이즈 270. 실착 5회 미만입니다.")
                                .price(85000)
                                .category(clothing)
                                .seller(user1)
                                .location("서울시 강남구")
                                .thumbnailUrl(cloudfrontDomain
                                                + "product-images/5f07ba0d-4ce9-4a99-a583-c7850ef3f69e.jpg")
                                .status(Product.Status.SOLD)
                                .build();
                p3.addImage(ProductImage.builder().imageUrl(p3.getThumbnailUrl()).orderIndex(0).build());
                p3.addImage(ProductImage.builder()
                                .imageUrl(cloudfrontDomain + "product-images/e9aab10b-b750-4337-9a16-3d4b503a59c0.jpg")
                                .orderIndex(1)
                                .build());

                Product p4 = Product.builder()
                                .title("맥북 에어 M2 13인치")
                                .description("램 16기가 모델입니다. 생활기스 약간 있어요.")
                                .price(1100000)
                                .category(digital)
                                .seller(user2)
                                .location("경기도 성남시")
                                .thumbnailUrl(cloudfrontDomain
                                                + "product-images/89dc51d4-427c-4705-9441-a326ae717759.jpg")
                                .status(Product.Status.ON_SALE)
                                .build();
                p4.addImage(ProductImage.builder().imageUrl(p4.getThumbnailUrl()).orderIndex(0).build());
                p4.addImage(ProductImage.builder()
                                .imageUrl(cloudfrontDomain + "product-images/fa9be452-e3ca-44e4-ad6a-7a4528152b37.jpg")
                                .orderIndex(1)
                                .build());

                productRepository.saveAll(List.of(p1, p2, p3, p4));

                log.info("Successfully initialized 4 products");
        }
}
