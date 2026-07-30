package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.dto.DailyViewStat;
import com.example.nutriuniv.domain.logging.entity.ProductViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductViewLogRepository extends JpaRepository<ProductViewLog, Long> {

    // 관리자 조회 통계 (일별 조회수 / 고유 상품 수)
    // native query + Object[] 대신 JPQL 생성자 표현식을 사용한다.
    @Query("""
            SELECT new com.example.nutriuniv.domain.logging.dto.DailyViewStat(
                       CAST(v.createdAt AS LocalDate),
                       COUNT(v),
                       COUNT(DISTINCT v.product.id))
            FROM ProductViewLog v
            WHERE v.createdAt >= :start AND v.createdAt < :end
            GROUP BY CAST(v.createdAt AS LocalDate)
            ORDER BY CAST(v.createdAt AS LocalDate)
            """)
    List<DailyViewStat> findDailyStats(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}