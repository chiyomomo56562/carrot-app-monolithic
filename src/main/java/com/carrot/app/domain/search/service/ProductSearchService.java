package com.carrot.app.domain.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrot.app.domain.product.entity.Product;
import com.carrot.app.domain.product.repository.ProductRepository;
import com.carrot.app.domain.search.document.ProductDocument;
import com.carrot.app.domain.search.dto.SearchProductResponse;
import com.carrot.app.domain.search.repository.ProductSearchRepository;
import com.carrot.app.global.exception.ProductNotFoundException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.domain.PageImpl;

import java.util.stream.Collectors;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;

    // Search products with filters
    @Transactional(readOnly = true)
    public Page<SearchProductResponse> searchProducts(
            String keyword,
            Long categoryId,
            String status,
            Integer minPrice,
            Integer maxPrice,
            Pageable pageable) {

        Criteria criteria = new Criteria();

        // 1. Keyword Search
        if (keyword != null && !keyword.isBlank()) {
            criteria = criteria.subCriteria(
                    new Criteria("title").matches(keyword)
                            .or(new Criteria("description").matches(keyword)));
        }

        // 2. Category Filter
        if (categoryId != null) {
            criteria = criteria.and("categoryId").is(categoryId);
        }

        // 3. Status Filter
        if (status != null && !status.isBlank()) {
            criteria = criteria.and("status").is(status);
        }

        // 4. Price Range Filter
        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = new Criteria("price");
            if (minPrice != null) {
                priceCriteria = priceCriteria.greaterThanEqual(minPrice);
            }
            if (maxPrice != null) {
                priceCriteria = priceCriteria.lessThanEqual(maxPrice);
            }
            criteria = criteria.subCriteria(priceCriteria);
        }

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(pageable);

        SearchHits<ProductDocument> searchHits = elasticsearchOperations
                .search(query,
                        ProductDocument.class);

        // Convert SearchHits to Page
        List<SearchProductResponse> responses = searchHits.stream()
                .map(hit -> SearchProductResponse.from(hit.getContent()))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, searchHits.getTotalHits());
    }

    // Index a product
    @Transactional(readOnly = true) // Read form DB
    public void indexProduct(Long productId) {
        log.info("### Indexing product: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found for indexing: " + productId));

        ProductDocument document = ProductDocument.from(product);
        productSearchRepository.save(document);
        log.info("### Product indexed: {}", productId);
    }

    // Delete a product from index
    public void deleteProduct(Long productId) {
        log.info("### Deleting product from index: {}", productId);
        productSearchRepository.deleteById(productId);
        log.info("### Product deleted from index: {}", productId);
    }
}
