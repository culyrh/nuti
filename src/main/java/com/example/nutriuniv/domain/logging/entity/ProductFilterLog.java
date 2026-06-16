package com.example.nutriuniv.domain.logging.entity;

import com.example.nutriuniv.domain.logging.dto.LogContext;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * compare_or_filter_used — 필터/정렬/등급정렬 사용 이벤트.
 * 활성화·유효방문에는 포함하지 않는 "핵심 행동" 지표.
 */
@Entity
@Table(name = "product_filter_logs",
        indexes = @Index(name = "idx_filter_logs_anon", columnList = "anonymous_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProductFilterLog {

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

    // 필터 종류: category / brand / sort / grade_sort 등
    @Column(name = "filter_type", nullable = false, length = 30)
    private String filterType;

    // 선택된 값(선택): 정렬키, 카테고리ID 등 — 분석 보조용
    @Column(name = "filter_value", length = 100)
    private String filterValue;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ProductFilterLog create(LogContext ctx, String filterType, String filterValue) {
        ProductFilterLog log = new ProductFilterLog();
        log.userId = ctx.userId();
        log.anonymousId = ctx.anonymousId();
        log.sessionId = ctx.sessionId();
        log.cohort = ctx.cohort();
        log.filterType = filterType;
        log.filterValue = filterValue;
        return log;
    }
}
