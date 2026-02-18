package com.carrot.app.domain.product.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.carrot.app.domain.product.dto.ProductCreateRequest;
import com.carrot.app.domain.product.dto.ProductResponse;
import com.carrot.app.domain.product.dto.ProductUpdateRequest;
import com.carrot.app.domain.product.service.ProductService;
import com.carrot.app.global.exception.FileSizeException;
import com.carrot.app.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // 1. 상품 등록 기능
    @PreAuthorize("hasRole('USER')")
    @PostMapping(value = "/new", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @ModelAttribute ProductCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) { // JWT filter puts email or userDetails
        log.info("### ProductController: {}", userDetails.getUsername());

        if (request.getImages() != null) {
            for (MultipartFile file : request.getImages()) {
                if (file.getSize() > MAX_FILE_SIZE) {
                    throw new FileSizeException("이미지 파일의 크기는 10MB 이하로 설정해주세요.");
                }
            }
        }
        log.info("### ProductController: 이미지 파일 크기 확인 완료");
        return ResponseEntity.ok(productService.createProduct(request, userDetails.getUsername()));
    }

    // 2. 상품 정보 수정 기능
    @PreAuthorize("hasRole('USER')")
    @PatchMapping(value = "/{productId}/edit", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable(name = "productId") Long productId,
            @Valid @ModelAttribute ProductUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(productService.updateProduct(productId, request, userDetails.getUsername()));
    }
}
