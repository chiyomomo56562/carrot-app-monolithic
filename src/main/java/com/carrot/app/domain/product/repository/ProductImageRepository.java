package com.carrot.app.domain.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carrot.app.domain.product.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

}
