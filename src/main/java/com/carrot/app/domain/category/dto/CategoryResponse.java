package com.carrot.app.domain.category.dto;

import com.carrot.app.domain.category.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private Long parentId;
    private Integer depth;
    private Integer displayOrder;
    private List<CategoryResponse> children;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .depth(category.getDepth())
                .displayOrder(category.getDisplayOrder())
                .children(category.getChildren().stream()
                        .map(CategoryResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
