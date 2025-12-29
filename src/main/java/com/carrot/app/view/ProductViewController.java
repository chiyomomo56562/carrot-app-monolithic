package com.carrot.app.view;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.carrot.app.domain.category.service.CategoryService;
import com.carrot.app.domain.product.dto.ProductDetailResponse;
import com.carrot.app.domain.product.dto.ProductResponse;
import com.carrot.app.domain.product.service.ProductService;
import com.carrot.app.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;

    // 2. 상품 목록 조회 (검색 기능 없는 간단한 조회, 태그별로 보기 기능)
    @GetMapping("")
    public String getProducts(
            Model model,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> products = productService.getProducts(categoryId, pageable);
        model.addAttribute("products", products);
        return "products/list";
    }

    // 3. 상품 상세 정보 조회 기능
    @GetMapping("/{productId}")
    public String getProductDetail(
            Model model,
            @PathVariable Long productId) {
        ProductDetailResponse productDetail = productService.getProductDetail(productId);
        model.addAttribute("product", productDetail);
        return "products/detail";
    }

    // 상품 등록 페이지
    @GetMapping("/new")
    public String productForm(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "products/form";
    }

    // 상품 수정 페이지
    @GetMapping("/{productId}/edit")
    public String productEditPage(@PathVariable Long productId, Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProductDetailResponse productDetail = productService.getProductDetail(productId);
        model.addAttribute("product", productDetail);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("statuses", com.carrot.app.domain.product.entity.Product.Status.values());
        return "products/edit";
    }
}