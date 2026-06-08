package com.example.nutriuniv.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class RecommendationResponse {

    /**
     * CF    — 협업 필터링 (행동 로그 기반 ALS)
     * CONTENT — 콘텐츠 기반 (diet_purpose + 영양소 벡터 유사도)
     * POPULAR — 인기순 폴백
     */
    private String type;
    private List<RecommendationItem> items;

    @Getter
    @Builder
    public static class RecommendationItem {
        private Long id;
        private String name;
        private String imageUrl;
        private BigDecimal nutritionScore;
        private String grade;       // A~E 등급 (PNS 미계산 상품이면 null)
        private boolean isFavorited;
        private Double score;       // CF일 때만 값 존재, 나머지는 null
        private BrandInfo brand;
        private CategoryInfo category;
    }

    @Getter
    @Builder
    public static class BrandInfo {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String name;
    }
}
