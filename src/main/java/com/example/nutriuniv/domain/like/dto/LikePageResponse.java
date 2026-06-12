package com.example.nutriuniv.domain.like.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class LikePageResponse {

    private long total;
    private List<LikeItem> items;

    @Getter
    @Builder
    public static class LikeItem {
        private Long productId;
        private String name;
        private String imageUrl;
        private BigDecimal nutritionScore;
        private String grade;       // A~E 등급 (PNS 미계산 상품이면 null)
        private BrandInfo brand;
    }

    @Getter
    @Builder
    public static class BrandInfo {
        private Long id;
        private String name;
    }
}