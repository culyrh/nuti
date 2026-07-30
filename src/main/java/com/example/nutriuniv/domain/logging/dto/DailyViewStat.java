package com.example.nutriuniv.domain.logging.dto;

import java.time.LocalDate;

/**
 * 관리자 조회 통계 (GET /admin/stats/views) 일별 집계 결과.
 *
 * <p>JPQL 생성자 표현식으로 직접 매핑되므로, 필드 순서와 타입은
 * ProductViewLogRepository#findDailyStats 의 SELECT 절과 반드시 일치해야 합니다.
 * 불일치 시 애플리케이션 기동 시점에 실패합니다.
 */
public record DailyViewStat(
        LocalDate date,
        long viewCount,
        long uniqueProductCount
) {
}