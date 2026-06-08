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

/**
 * 상품 응답에 PNS 점수/등급/백분위를 채워주기 위한 조회 헬퍼.
 *
 * 비로그인 또는 eer_band 미입력 사용자는 기본 EER 2000 구간을 사용.
 */
@Service
@RequiredArgsConstructor
public class PnsLookupService {

    public static final int DEFAULT_EER_BAND = 2000;

    private final UserNutritionRepository userNutritionRepository;
    private final ProductPnsByEerRepository pnsRepository;
    private final EntityManager em;

    /** userId가 null 이거나 eer_band가 없으면 기본값(2000). */
    @Transactional(readOnly = true)
    public int resolveEerBand(Long userId) {
        if (userId == null) return DEFAULT_EER_BAND;
        return userNutritionRepository.findByUserId(userId)
                .map(UserNutrition::getEerBand)
                .filter(Objects::nonNull)
                .orElse(DEFAULT_EER_BAND);
    }

    /** 점수가 아직 계산 안 된 상품이면 null. */
    @Transactional(readOnly = true)
    public PnsLookupResult lookup(Long productId, int eerBand) {
        ProductPnsByEer pns = pnsRepository.findById(
                new ProductPnsByEer.PnsId(productId, eerBand)).orElse(null);
        if (pns == null) return null;

        BigDecimal percentile = pns.getPercentile();
        BigDecimal topPercent = percentile == null
                ? null
                : BigDecimal.valueOf(100).subtract(percentile).setScale(2, java.math.RoundingMode.HALF_UP);

        return new PnsLookupResult(
                pns.getScore(),
                pns.getGrade(),
                percentile,
                topPercent,
                eerBand
        );
    }

    /** 대분류 안의 활성 상품 수. parentCategoryId가 null이면 0. */
    @Transactional(readOnly = true)
    public int countActiveByParentCategory(Long parentCategoryId) {
        if (parentCategoryId == null) return 0;
        String sql = """
            SELECT COUNT(*)
            FROM   products p
            JOIN   categories c ON p.category_id = c.id
            WHERE  c.parent_id = :pid
              AND  p.is_active = TRUE
            """;
        Number cnt = (Number) em.createNativeQuery(sql)
                .setParameter("pid", parentCategoryId)
                .getSingleResult();
        return cnt == null ? 0 : cnt.intValue();
    }

    /** 상품 목록용 — productId 리스트를 한 번에 조회해서 Map&lt;productId, grade&gt; 반환. */
    @Transactional(readOnly = true)
    public Map<Long, String> lookupGrades(List<Long> productIds, int eerBand) {
        return pnsRepository.findGradesByProductIdsAndEerBand(productIds, eerBand);
    }

    public record PnsLookupResult(
            BigDecimal score,
            String grade,
            BigDecimal percentile,
            BigDecimal topPercent,
            int eerBand
    ) {}
}
