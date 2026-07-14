package com.example.nutriuniv.domain.logging.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 상품 노출(impression) — 목록/추천/검색결과에 "보여진" 기록.
 *
 * <p>view(클릭)와 달리 클릭하지 않아도 남긴다. 추천 모델 학습 시
 * "노출됐지만 view/찜 없음 = 음성 샘플(hard negative)"로 사용한다.
 * 활성화·유효방문에는 포함하지 않는다(세션도 건드리지 않음).
 */
@Entity
@Table(name = "product_impression_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_impr_session_product_surface",
                columnNames = {"session_id", "product_id", "surface"}),
        indexes = {
                @Index(name = "idx_impr_product", columnList = "product_id"),
                @Index(name = "idx_impr_created", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProductImpressionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "anonymous_id", length = 36)
    private String anonymousId;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "cohort", length = 10)
    private String cohort;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // 노출 위치: LIST / RECOMMENDATION / SEARCH
    @Column(name = "surface", nullable = false, length = 20)
    private String surface;

    // 목록 내 순위(선택)
    @Column(name = "position")
    private Integer position;

    // 검색 노출이면 검색어(선택)
    @Column(name = "keyword", length = 30)
    private String keyword;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
