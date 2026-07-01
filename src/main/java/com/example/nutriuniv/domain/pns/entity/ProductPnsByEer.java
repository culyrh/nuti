package com.example.nutriuniv.domain.pns.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PK: (product_id, eer_band, goal)
 *
 * 변경점 (v4.1):
 *  - goal 컬럼 추가 ("diet" | "bulk") → PK에 포함
 *  - health_score 컬럼 추가
 *  - score 범위: 0~100 (기존 raw 값 아님)
 */
@Entity
@Table(name = "product_pns_by_eer",
        indexes = {
                @Index(name = "idx_pns_goal_band_grade",      columnList = "goal, eer_band, grade"),
                @Index(name = "idx_pns_goal_band_percentile", columnList = "goal, eer_band, percentile DESC")
        })
@IdClass(ProductPnsByEer.PnsId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductPnsByEer {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Id
    @Column(name = "eer_band")
    private Integer eerBand;

    @Id
    @Column(name = "goal", length = 10)
    private String goal;           // "diet" | "bulk"

    @Column(precision = 6, scale = 1, nullable = false)
    private BigDecimal score;      // 0.0 ~ 100.0

    @Column(length = 1, nullable = false)
    private String grade;          // A~E

    @Column(name = "health_score", precision = 6, scale = 1)
    private BigDecimal healthScore; // 0.0 ~ 100.0

    @Column(precision = 5, scale = 2)
    private BigDecimal percentile;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ProductPnsByEer create(Long productId, int eerBand, String goal,
                                         double score, String grade, double healthScore) {
        ProductPnsByEer p = new ProductPnsByEer();
        p.productId   = productId;
        p.eerBand     = eerBand;
        p.goal        = goal;
        p.score       = toBD(score, 1);
        p.grade       = grade;
        p.healthScore = toBD(healthScore, 1);
        p.updatedAt   = LocalDateTime.now();
        return p;
    }

    public void updatePercentile(double percentile) {
        this.percentile = BigDecimal.valueOf(percentile).setScale(2, RoundingMode.HALF_UP);
        this.updatedAt  = LocalDateTime.now();
    }

    private static BigDecimal toBD(double v, int scale) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return BigDecimal.ZERO;
        double clamped = Math.max(0.0, Math.min(100.0, v));
        return BigDecimal.valueOf(clamped).setScale(scale, RoundingMode.HALF_UP);
    }

    // ── 복합 PK ───────────────────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PnsId implements Serializable {
        private Long    productId;
        private Integer eerBand;
        private String  goal;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PnsId that)) return false;
            return Objects.equals(productId, that.productId)
                    && Objects.equals(eerBand,   that.eerBand)
                    && Objects.equals(goal,      that.goal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, eerBand, goal);
        }
    }
}