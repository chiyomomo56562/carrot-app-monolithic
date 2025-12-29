package com.carrot.app.domain.category.service;

import com.carrot.app.domain.category.dto.CategoryResponse;
import com.carrot.app.domain.category.entity.Category;
import com.carrot.app.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 카테고리 전체 목록 조회 (트리 구조)
    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        // 루트 카테고리만 가져오면, Response DTO의 from 메서드에서 재귀적으로 자식들을 변환함
        List<Category> rootCategories = categoryRepository.findAllByParentIsNullOrderByDisplayOrderAsc();

        return rootCategories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }
}
