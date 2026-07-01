package com.example.nutriuniv.domain.pns.service;

import com.example.nutriuniv.domain.pns.calculator.PnsCalculator;
import com.example.nutriuniv.domain.pns.entity.ProductPnsByEer;
import com.example.nutriuniv.domain.pns.repository.ProductPnsByEerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * PNS 배치 계산 서비스 — 명세 v4.1
 *
 * 변경점:
 *  - goal 차원 추가 (diet / bulk) → EER 4구간 × goal 2개 = 8개 조합 저장
 *  - 100g당 기준 컬럼(*_per_100g)으로 쿼리 교체
 *  - MealRatioResolver 제거
 *  - health_score 함께 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PnsBatchService {

    private static final int[]    EER_BANDS = {1500, 2000, 2500, 3000};
    private static final String[] GOALS     = {"diet", "bulk"};

    private final ProductPnsByEerRepository repository;

    @Transactional
    public BatchResult calculateAll() {
        long startMs = System.currentTimeMillis();

        List<Object[]> rows = repository.fetchProductsWithNutrientsPer100g();
        log.info("[PNS] {}개 상품 로드", rows.size());

        if (rows.isEmpty()) {
            return new BatchResult(0, 0, EER_BANDS.length * GOALS.length, 0L);
        }

        int totalSaved = 0;

        for (String goal : GOALS) {
            for (int band : EER_BANDS) {

                int deleted = repository.deleteByGoalAndEerBand(goal, band);
                log.info("[PNS] goal={} band={} 기존 {}건 삭제", goal, band, deleted);

                List<ProductPnsByEer> entities  = new ArrayList<>(rows.size());
                Map<Long, List<ProductPnsByEer>> byParent = new HashMap<>();

                for (Object[] r : rows) {
                    Long productId      = ((Number) r[0]).longValue();
                    Long parentCategory = r[1] == null ? null : ((Number) r[1]).longValue();

                    BigDecimal calories = (BigDecimal) r[2];
                    BigDecimal protein  = (BigDecimal) r[3];
                    BigDecimal fiber    = (BigDecimal) r[4];
                    BigDecimal sugar    = (BigDecimal) r[5];
                    BigDecimal satFat   = (BigDecimal) r[6];
                    BigDecimal transFat = (BigDecimal) r[7];
                    BigDecimal sodium   = (BigDecimal) r[8];
                    BigDecimal chol     = (BigDecimal) r[9];

                    PnsCalculator.Result res = PnsCalculator.calculate(
                            goal, band,
                            calories, protein, fiber,
                            sugar, satFat, transFat,
                            sodium, chol
                    );

                    ProductPnsByEer entity = ProductPnsByEer.create(
                            productId, band, goal,
                            res.score, res.grade, res.healthScore
                    );
                    entities.add(entity);
                    byParent.computeIfAbsent(parentCategory, k -> new ArrayList<>()).add(entity);
                }

                // 대분류별 백분위 산출
                for (List<ProductPnsByEer> group : byParent.values()) {
                    group.sort(Comparator.comparing(ProductPnsByEer::getScore).reversed());
                    int total = group.size();
                    for (int i = 0; i < total; i++) {
                        group.get(i).updatePercentile(((double)(total - i)) / total * 100.0);
                    }
                }

                repository.saveAll(entities);
                totalSaved += entities.size();
                log.info("[PNS] goal={} band={} {}건 저장", goal, band, entities.size());
            }
        }

        long elapsed = System.currentTimeMillis() - startMs;
        log.info("[PNS] 완료 — 상품 {}개 × {}개 조합 = {}건 ({}ms)",
                rows.size(), EER_BANDS.length * GOALS.length, totalSaved, elapsed);

        return new BatchResult(rows.size(), totalSaved, EER_BANDS.length * GOALS.length, elapsed);
    }

    public record BatchResult(int productCount, int savedRows, int combinations, long elapsedMs) {}
}