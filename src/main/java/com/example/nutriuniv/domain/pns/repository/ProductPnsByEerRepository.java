package com.example.nutriuniv.domain.pns.repository;

import com.example.nutriuniv.domain.pns.entity.ProductPnsByEer;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface ProductPnsByEerRepository
        extends JpaRepository<ProductPnsByEer, ProductPnsByEer.PnsId>,
                ProductPnsByEerCustom {

    @Modifying
    @Query("DELETE FROM ProductPnsByEer p WHERE p.eerBand = :band")
    int deleteByEerBand(int band);
}

interface ProductPnsByEerCustom {
    /** product → category(depth=2) → category(depth=1) parent_id 매핑. */
    List<Object[]> fetchProductsWithParentCategory();

    /** 상품 ID 목록 + EER 밴드로 grade를 한 번에 조회. Map<productId, grade> 반환. */
    Map<Long, String> findGradesByProductIdsAndEerBand(List<Long> productIds, int eerBand);
}

@RequiredArgsConstructor
class ProductPnsByEerCustomImpl implements ProductPnsByEerCustom {

    private final EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, String> findGradesByProductIdsAndEerBand(List<Long> productIds, int eerBand) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        String sql = """
            SELECT p.product_id, p.grade
            FROM   product_pns_by_eer p
            WHERE  p.product_id IN (:ids)
              AND  p.eer_band   = :band
            """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("ids", productIds)
                .setParameter("band", eerBand)
                .getResultList();
        Map<Long, String> result = new java.util.HashMap<>();
        for (Object[] row : rows) {
            result.put(((Number) row[0]).longValue(), (String) row[1]);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> fetchProductsWithParentCategory() {
        // 반환: [product_id, parent_category_id,
        //        carb, protein, fat, fiber, cholesterol,
        //        satFat, transFat, sugar, sodium]
        String sql = """
            SELECT p.id,
                   c.parent_id,
                   pn.carbohydrate, pn.protein, pn.fat,
                   pn.dietary_fiber, pn.cholesterol,
                   pn.saturated_fat, pn.trans_fat,
                   pn.sugar, pn.sodium
            FROM   products p
            JOIN   categories c        ON p.category_id = c.id
            JOIN   product_nutrients pn ON pn.product_id = p.id
            WHERE  p.is_active = TRUE
              AND  c.parent_id IS NOT NULL
              AND  pn.calories IS NOT NULL
            """;
        return em.createNativeQuery(sql).getResultList();
    }
}
