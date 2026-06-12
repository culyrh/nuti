package com.example.nutriuniv.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductListResponse {

    // 목록 조회 응답 (items 안에 들어가는 것)
    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal nutritionScore;
    private String grade;           // A~E 등급 (PNS 미계산 상품이면 null)
    private Integer price;          // 쿠팡 LINKED 상품만 값 존재, 나머지 null
    private boolean isFavorited;
    private BrandInfo brand;
    private CategoryInfo category;

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