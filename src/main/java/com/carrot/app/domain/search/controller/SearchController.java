package com.carrot.app.domain.search.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.carrot.app.domain.search.dto.SearchProductResponse;
import com.carrot.app.domain.search.service.ProductSearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchController {

        private final ProductSearchService productSearchService;
        private final com.carrot.app.domain.category.repository.CategoryRepository categoryRepository;

        @GetMapping("/search")
        public String search(
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) Integer minPrice,
                        @RequestParam(required = false) Integer maxPrice,
                        @PageableDefault(size = 20) Pageable pageable,
                        Model model) {

                log.info("### Search request: q={}, category={}, status={}, price={}~{}, page={}",
                                q, categoryId, status, minPrice, maxPrice, pageable.getPageNumber());

                Page<SearchProductResponse> results = productSearchService.searchProducts(q, categoryId, status,
                                minPrice,
                                maxPrice, pageable);

                model.addAttribute("products", results);
                model.addAttribute("keyword", q);
                model.addAttribute("categoryId", categoryId);
                model.addAttribute("status", status);
                model.addAttribute("minPrice", minPrice);
                model.addAttribute("maxPrice", maxPrice);

                model.addAttribute("categories", categoryRepository.findAll()); // Ideally cache this or hierarchically
                                                                                // fetch

                return "products/search";
        }
}
