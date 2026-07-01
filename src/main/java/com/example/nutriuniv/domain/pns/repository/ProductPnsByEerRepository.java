package com.example.nutriuniv.domain.pns.repository;

import com.example.nutriuniv.domain.pns.entity.ProductPnsByEer;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface ProductPnsByEerRepository
        extends JpaRepository<ProductPnsByEer, ProductPnsByEer.PnsId>,
        ProductPnsByEerCustom {

    @Modifying
    @Query("DELETE FROM ProductPnsByEer p WHERE p.goal = :goal AND p.eerBand = :band")
    int deleteByGoalAndEerBand(@Param("goal") String goal, @Param("band") int band);
}

interface ProductPnsByEerCustom {
    List<Object[]> fetchProductsWithNutrientsPer100g();
    Map<Long, String> findGradesByProductIdsAndEerBandAndGoal(List<Long> productIds, int eerBand, String goal);
    ProductPnsByEer findOneByProductIdAndEerBandAndGoal(Long productId, int eerBand, String goal);
}

@RequiredArgsConstructor
class ProductPnsByEerCustomImpl implements ProductPnsByEerCustom {

    private final EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> fetchProductsWithNutrientsPer100g() {
        String sql = """
                SELECT p.id,
                       c.parent_id,
                       pn.calories_per_100g,
                       pn.protein_per_100g,
                       pn.dietary_fiber_per_100g,
                       pn.sugar_per_100g,
                       pn.saturated_fat_per_100g,
                       pn.trans_fat_per_100g,
                       pn.sodium_per_100g,
                       pn.cholesterol_per_100g
                FROM   products p
                JOIN   categories c         ON p.category_id  = c.id
                JOIN   product_nutrients pn ON pn.product_id  = p.id
                WHERE  p.is_active = TRUE
                  AND  c.parent_id IS NOT NULL
                  AND  pn.calories_per_100g IS NOT NULL
                """;
        return em.createNativeQuery(sql).getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, String> findGradesByProductIdsAndEerBandAndGoal(
            List<Long> productIds, int eerBand, String goal) {

        if (productIds == null || productIds.isEmpty()) return Map.of();

        // grade를 CAST로 명시적으로 VARCHAR 변환 → Character 캐스팅 오류 방지
        String sql = """
                SELECT p.product_id, CAST(p.grade AS VARCHAR)
                FROM   product_pns_by_eer p
                WHERE  p.product_id = ANY(?1)
                  AND  p.eer_band   = ?2
                  AND  p.goal       = ?3
                """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, productIds.toArray(new Long[0]))
                .setParameter(2, eerBand)
                .setParameter(3, goal)
                .getResultList();

        Map<Long, String> result = new java.util.HashMap<>();
        for (Object[] row : rows) {
            result.put(((Number) row[0]).longValue(), String.valueOf(row[1]));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProductPnsByEer findOneByProductIdAndEerBandAndGoal(
            Long productId, int eerBand, String goal) {

        String sql = """
                SELECT *
                FROM   product_pns_by_eer
                WHERE  product_id = ?1
                  AND  eer_band   = ?2
                  AND  goal       = ?3
                """;
        List<ProductPnsByEer> rows = em.createNativeQuery(sql, ProductPnsByEer.class)
                .setParameter(1, productId)
                .setParameter(2, eerBand)
                .setParameter(3, goal)
                .getResultList();

        return rows.isEmpty() ? null : rows.get(0);
    }
}