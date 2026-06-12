package com.example.nutriuniv.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal nutritionScore;
    private String grade;           // A~E 등급 (PNS 미계산 상품이면 null)
    private int viewCount;
    private boolean isFavorited;
    private Double scoreRankPercent;
    private BrandInfo brand;
    private CategoryInfo category;
    private NutrientInfo nutrients;
    private CoupangInfo coupang;   // 나중에 채울 예정, 지금은 null
    private PnsInfo pns;            // 점수 미계산 상품이면 null
    private NutrientBounds nutrientBounds;  // 영양소 바 차트 기준값

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

    @Getter
    @Builder
    public static class NutrientInfo {
        private String servingSize;
        private BigDecimal calories;
        private BigDecimal carbohydrate;
        private BigDecimal sugar;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal saturatedFat;
        private BigDecimal transFat;
        private BigDecimal cholesterol;
        private BigDecimal sodium;
    }

    @Getter
    @Builder
    public static class CoupangInfo {
        private String affiliateUrl;
        private String landingUrl;
        private Integer price;
        private Boolean isRocket;
        private Boolean isFreeShipping;
        private LocalDateTime lastSyncedAt;
    }

    /**
     * 영양소 바 차트용 기준값 (사용자 EER 기반 Nmin/Nmax).
     * 비로그인 시 EER 2000 기준으로 계산.
     * null 값은 해당 영양소에 Nmin 또는 Nmax 없음을 의미.
     */
    @Getter
    @Builder
    public static class NutrientBounds {
        private double eerUsed;          // 계산에 사용된 EER (kcal)
        private double mealRatio;        // 적용된 meal_ratio (0.1 or 0.3)

        // 열량: 0 ~ EER×mealRatio
        private double caloriesMax;

        // 탄수화물: Nmin ~ Nmax
        private double carbMin;
        private double carbMax;

        // 단백질: Nmin ~ Nmax
        private double proteinMin;
        private double proteinMax;

        // 지방: Nmin ~ Nmax
        private double fatMin;
        private double fatMax;

        // 당류: 0 ~ Nmax
        private double sugarMax;

        // 포화지방: 0 ~ Nmax
        private double saturatedFatMax;

        // 트랜스지방: 0 ~ Nmax
        private double transFatMax;

        // 콜레스테롤: 0 ~ 300×mealRatio
        private double cholesterolMax;

        // 나트륨: 0 ~ Nmax (2300×mealRatio)
        private double sodiumMax;

        // 식이섬유: 0 ~ Nmin (목표값)
        private double fiberTarget;
    }

    @Getter
    @Builder
    public static class PnsInfo {
        private BigDecimal score;            // PFS 원점수 (-∞ ~ +9)
        private String grade;                // A~E
        private BigDecimal percentile;       // 카테고리 내 백분위 (0~100, 클수록 좋음)
        private BigDecimal topPercent;       // 상위 X% (= 100 - percentile)
        private Long parentCategoryId;       // 대분류 ID
        private String parentCategoryName;   // 대분류 이름 (예: "음료류")
        private int categoryTotal;           // 대분류 안 활성 상품 수
        private int eerBand;                 // 사용된 EER 구간 (1500/2000/2500/3000)
    }
}