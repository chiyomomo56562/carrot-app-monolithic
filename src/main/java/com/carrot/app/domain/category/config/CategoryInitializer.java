package com.carrot.app.domain.category.config;

import com.carrot.app.domain.category.entity.Category;
import com.carrot.app.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 카테고리가 존재하면 초기화하지 않음
        if (categoryRepository.count() > 0) {
            log.info("Categories already exist. Skipping initialization.");
            return;
        }

        log.info("Initializing categories...");

        List<Category> categories = new ArrayList<>();
        int displayOrder = 0;

        // 1. 디지털기기
        Category digitalDevices = createCategory("디지털기기", null, 0, displayOrder++);
        categories.add(digitalDevices);
        categories.add(createCategory("휴대폰", digitalDevices, 1, 0));
        categories.add(createCategory("태블릿", digitalDevices, 1, 1));
        categories.add(createCategory("노트북", digitalDevices, 1, 2));

        // 2. 생활가전
        Category homeAppliances = createCategory("생활가전", null, 0, displayOrder++);
        categories.add(homeAppliances);
        categories.add(createCategory("TV", homeAppliances, 1, 0));
        categories.add(createCategory("냉장고", homeAppliances, 1, 1));
        categories.add(createCategory("세탁기", homeAppliances, 1, 2));

        // 3. 가구/인테리어
        Category furniture = createCategory("가구/인테리어", null, 0, displayOrder++);
        categories.add(furniture);
        categories.add(createCategory("침대", furniture, 1, 0));
        categories.add(createCategory("소파", furniture, 1, 1));
        categories.add(createCategory("책상/의자", furniture, 1, 2));

        // 4. 생활/주방
        Category living = createCategory("생활/주방", null, 0, displayOrder++);
        categories.add(living);
        categories.add(createCategory("주방용품", living, 1, 0));
        categories.add(createCategory("생활용품", living, 1, 1));

        // 5. 의류
        Category clothing = createCategory("의류", null, 0, displayOrder++);
        categories.add(clothing);
        categories.add(createCategory("남성의류", clothing, 1, 0));
        categories.add(createCategory("여성의류", clothing, 1, 1));
        categories.add(createCategory("아동의류", clothing, 1, 2));

        // 6. 도서
        categories.add(createCategory("도서", null, 0, displayOrder++));

        // 7. 기타
        categories.add(createCategory("기타", null, 0, displayOrder++));

        // 모든 카테고리 저장
        categoryRepository.saveAll(categories);

        log.info("Successfully initialized {} categories", categories.size());
    }

    private Category createCategory(String name, Category parent, int depth, int displayOrder) {
        return Category.builder()
                .name(name)
                .parent(parent)
                .depth(depth)
                .displayOrder(displayOrder)
                .build();
    }
}
