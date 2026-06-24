package com.example.nutriuniv.domain.review.repository;

import com.example.nutriuniv.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 상품별 활성 리뷰 페이징
    Page<Review> findByProductIdAndIsActiveTrue(Long productId, Pageable pageable);

    // 중복 리뷰 체크 (삭제 이력 포함 — 재작성 영구 불가)
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // 본인 리뷰 조회 (수정/삭제 시 권한 확인용)
    Optional<Review> findByIdAndIsActiveTrue(Long id);

    // 평균 점수 집계
    @Query("""
            SELECT AVG(r.scoreOverall) FROM Review r
            WHERE r.product.id = :productId AND r.isActive = true
            """)
    Double avgScoreOverall(@Param("productId") Long productId);
}