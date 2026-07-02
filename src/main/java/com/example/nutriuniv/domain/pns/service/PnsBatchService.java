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
 * goal:
 *  - diet  × EER 4구간 = 4개 조합
 *  - bulk  × EER 4구간 = 4개 조합
 *  - health × EER 1구간(2000 고정) = 1개 조합 (EER 무관, health_score 기준)
 * 총 9개 조합 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PnsBatchService {

    private static final int[]    EER_BANDS   = {1500, 2000, 2500, 3000};
    private static final String[] DIET_BULK   = {"diet", "bulk"};
    private static final int      HEALTH_BAND = 2000; // health는 EER 무관, 대표값으로 저장

    private final ProductPnsByEerRepository repository;

    @Transactional
    public BatchResult calculateAll() {
        long startMs = System.currentTimeMillis();

        List<Object[]> rows = repository.fetchProductsWithNutrientsPer100g();
        log.info("[PNS] {}개 상품 로드", rows.size());

        if (rows.isEmpty()) {
            return new BatchResult(0, 0, 0, 0L);
        }

        int totalSaved = 0;

        // ── diet / bulk × 4구간 ───────────────────────────────────────────────
        for (String goal : DIET_BULK) {
            for (int band : EER_BANDS) {

                int deleted = repository.deleteByGoalAndEerBand(goal, band);
                log.info("[PNS] goal={} band={} 기존 {}건 삭제", goal, band, deleted);

                List<ProductPnsByEer> entities = new ArrayList<>(rows.size());
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

        // ── health × 1구간(2000 고정) ─────────────────────────────────────────
        int deleted = repository.deleteByGoalAndEerBand("health", HEALTH_BAND);
        log.info("[PNS] goal=health band={} 기존 {}건 삭제", HEALTH_BAND, deleted);

        List<ProductPnsByEer> healthEntities = new ArrayList<>(rows.size());
        Map<Long, List<ProductPnsByEer>> byParentHealth = new HashMap<>();

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

            // health는 goal/EER 무관 — diet 2000으로 계산 후 healthScore/healthGrade만 사용
            PnsCalculator.Result res = PnsCalculator.calculate(
                    "diet", HEALTH_BAND,
                    calories, protein, fiber,
                    sugar, satFat, transFat,
                    sodium, chol
            );

            String healthGrade = PnsCalculator.toHealthGrade(res.healthScore);

            ProductPnsByEer entity = ProductPnsByEer.create(
                    productId, HEALTH_BAND, "health",
                    res.healthScore, healthGrade, res.healthScore
            );
            healthEntities.add(entity);
            byParentHealth.computeIfAbsent(parentCategory, k -> new ArrayList<>()).add(entity);
        }

        // 대분류별 백분위 산출
        for (List<ProductPnsByEer> group : byParentHealth.values()) {
            group.sort(Comparator.comparing(ProductPnsByEer::getScore).reversed());
            int total = group.size();
            for (int i = 0; i < total; i++) {
                group.get(i).updatePercentile(((double)(total - i)) / total * 100.0);
            }
        }

        repository.saveAll(healthEntities);
        totalSaved += healthEntities.size();
        log.info("[PNS] goal=health band={} {}건 저장", HEALTH_BAND, healthEntities.size());

        long elapsed = System.currentTimeMillis() - startMs;
        log.info("[PNS] 완료 — 상품 {}개 × 9개 조합 = {}건 ({}ms)", rows.size(), totalSaved, elapsed);

        return new BatchResult(rows.size(), totalSaved, 9, elapsed);
    }

    public record BatchResult(int productCount, int savedRows, int combinations, long elapsedMs) {}
}