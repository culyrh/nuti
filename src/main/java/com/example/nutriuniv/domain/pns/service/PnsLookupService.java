package com.example.nutriuniv.domain.pns.service;

import com.example.nutriuniv.domain.pns.entity.ProductPnsByEer;
import com.example.nutriuniv.domain.pns.repository.ProductPnsByEerRepository;
import com.example.nutriuniv.domain.user.entity.UserNutrition;
import com.example.nutriuniv.domain.user.repository.UserNutritionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PnsLookupService {

    public static final int    DEFAULT_EER_BAND = 2000;
    public static final String DEFAULT_GOAL     = "diet";

    private final UserNutritionRepository   userNutritionRepository;
    private final ProductPnsByEerRepository pnsRepository;
    private final EntityManager             em;

    @Transactional(readOnly = true)
    public int resolveEerBand(Long userId) {
        if (userId == null) return DEFAULT_EER_BAND;
        return userNutritionRepository.findByUserId(userId)
                .map(UserNutrition::getEerBand)
                .filter(Objects::nonNull)
                .orElse(DEFAULT_EER_BAND);
    }

    /**
     * 사용자의 diet_purpose를 PNS goal로 변환.
     *  - "벌크업" → "bulk"
     *  - 그 외(다이어트 등) → "diet"
     *  - 비로그인 / 미입력 → "diet"
     */
    @Transactional(readOnly = true)
    public String resolveGoal(Long userId) {
        if (userId == null) return DEFAULT_GOAL;
        return userNutritionRepository.findByUserId(userId)
                .map(UserNutrition::getDietPurpose)
                .map(p -> "벌크업".equals(p) ? "bulk" : "diet")
                .orElse(DEFAULT_GOAL);
    }

    @Transactional(readOnly = true)
    public PnsLookupResult lookup(Long productId, int eerBand, String goal) {
        ProductPnsByEer pns = pnsRepository.findOneByProductIdAndEerBandAndGoal(productId, eerBand, goal);
        if (pns == null) return null;

        BigDecimal percentile = pns.getPercentile();
        BigDecimal topPercent = percentile == null
                ? null
                : BigDecimal.valueOf(100).subtract(percentile)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return new PnsLookupResult(
                pns.getScore(),
                pns.getGrade(),
                pns.getHealthScore(),
                percentile,
                topPercent,
                eerBand,
                goal
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, String> lookupGrades(List<Long> productIds, int eerBand, String goal) {
        return pnsRepository.findGradesByProductIdsAndEerBandAndGoal(productIds, eerBand, goal);
    }

    @Transactional(readOnly = true)
    public int countActiveByParentCategory(Long parentCategoryId) {
        if (parentCategoryId == null) return 0;

        // Native query positional parameter 사용
        String sql = """
                SELECT COUNT(*)
                FROM   products p
                JOIN   categories c ON p.category_id = c.id
                WHERE  c.parent_id = ?1
                  AND  p.is_active = TRUE
                """;
        Number cnt = (Number) em.createNativeQuery(sql)
                .setParameter(1, parentCategoryId)
                .getSingleResult();
        return cnt == null ? 0 : cnt.intValue();
    }

    public record PnsLookupResult(
            BigDecimal score,
            String     grade,
            BigDecimal healthScore,
            BigDecimal percentile,
            BigDecimal topPercent,
            int        eerBand,
            String     goal
    ) {}
}