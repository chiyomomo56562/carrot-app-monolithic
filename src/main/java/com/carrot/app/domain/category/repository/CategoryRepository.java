package com.carrot.app.domain.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.carrot.app.domain.category.entity.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 루트 카테고리 조회 (부모가 없는 카테고리)
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.displayOrder ASC")
    List<Category> findAllByParentIsNullOrderByDisplayOrderAsc();

    // 특정 부모의 하위 카테고리 조회
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId ORDER BY c.displayOrder ASC")
    List<Category> findAllByParentIdOrderByDisplayOrderAsc(Long parentId);

    Optional<Category> findByName(String name);
}
