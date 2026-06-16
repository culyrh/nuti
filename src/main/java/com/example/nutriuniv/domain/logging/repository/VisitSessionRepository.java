package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.entity.VisitSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VisitSessionRepository extends JpaRepository<VisitSession, Long> {

    Optional<VisitSession> findBySessionId(String sessionId);

    // 해당 anonymous_id의 첫 방문 시각 (Day 0). 없으면 null → 첫 세션.
    @Query("SELECT MIN(v.sessionStart) FROM VisitSession v WHERE v.anonymousId = :anonymousId")
    LocalDateTime findFirstSessionStart(@Param("anonymousId") String anonymousId);

    // ── 재방문 W2 합격 바 산출 ────────────────────────────────────────────────
    // W1(Day 0–6)에 유효 방문한 anonymous_id 중,
    // 4주 창(Day 7–27) 내 유효 방문이 1회 이상인 비율을 cohort별로 집계.
    // 결과: [cohort, w1_users, retained_users]
    @Query(value = """
            SELECT base.cohort                                   AS cohort,
                   COUNT(DISTINCT base.anonymous_id)             AS w1_users,
                   COUNT(DISTINCT ret.anonymous_id)              AS retained_users
            FROM (
                SELECT anonymous_id, MAX(cohort) AS cohort
                FROM visit_sessions
                WHERE qualified = true AND days_since_first BETWEEN 0 AND 6
                GROUP BY anonymous_id
            ) base
            LEFT JOIN (
                SELECT DISTINCT anonymous_id
                FROM visit_sessions
                WHERE qualified = true AND days_since_first BETWEEN 7 AND 27
            ) ret ON base.anonymous_id = ret.anonymous_id
            GROUP BY base.cohort
            """, nativeQuery = true)
    java.util.List<Object[]> findRetentionW2();
}
