package com.carrot.app.domain.search.repository;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carrot.app.domain.search.document.ProductDocument;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    @Query("""
            {
              "multi_match": {
                "query": "?0",
                "fields": ["title^2", "description"],
                "type": "best_fields"
              }
            }
            """)
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);
}
