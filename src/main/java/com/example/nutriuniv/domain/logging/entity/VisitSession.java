package com.example.nutriuniv.domain.logging.entity;

import com.example.nutriuniv.domain.logging.dto.LogContext;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 방문 세션 집계 — 재방문(return_visit) 측정의 핵심 테이블.
 *
 * <p>한 세션 = 한 행. 이벤트가 들어올 때마다 session_id로 upsert 된다.
 * 명세 B~H(세션·유효방문·active_days·주차·recency)를 이 테이블 하나로 사후 계산한다.
 *
 * <ul>
 *   <li>session = 30분 무활동 시 종료 (클라이언트가 session_id를 갱신)</li>
 *   <li>유효 방문(qualified) = 세션 내 search 또는 view 1회 이상</li>
 *   <li>days_since_first = 유저별 첫 방문일(Day 0) 상대 경과일 → 주차(W1~W4) 산출 기준</li>
 * </ul>
 */
@Entity
@Table(name = "visit_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_visit_sessions_session_id", columnNames = "session_id"),
        indexes = @Index(name = "idx_visit_sessions_anon", columnList = "anonymous_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "anonymous_id", nullable = false, length = 36)
    private String anonymousId;

    // 로그인 시 매핑 (비로그인은 null)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "cohort", length = 10)
    private String cohort;

    @Column(name = "session_start", nullable = false)
    private LocalDateTime sessionStart;

    @Column(name = "session_end", nullable = false)
    private LocalDateTime sessionEnd;

    // 유효 방문 여부 (search/view 발생 시 true)
    @Column(name = "qualified", nullable = false)
    private boolean qualified = false;

    // 이 anonymous_id의 첫 세션인지
    @Column(name = "is_first_session", nullable = false)
    private boolean isFirstSession = false;

    // 첫 방문일(Day 0) 기준 경과일 — 재방문 며칠째 / 주차 산출
    @Column(name = "days_since_first", nullable = false)
    private int daysSinceFirst = 0;

    public static VisitSession create(LogContext ctx, LocalDateTime now,
                                      boolean isFirstSession, int daysSinceFirst) {
        VisitSession s = new VisitSession();
        s.sessionId = ctx.sessionId();
        s.anonymousId = ctx.anonymousId();
        s.userId = ctx.userId();
        s.cohort = ctx.cohort();
        s.sessionStart = now;
        s.sessionEnd = now;
        s.isFirstSession = isFirstSession;
        s.daysSinceFirst = daysSinceFirst;
        return s;
    }

    /**
     * 동일 세션 내 후속 이벤트 반영: 종료시각 갱신, 유효방문 승격, 로그인 매핑 보강.
     */
    public void touch(LocalDateTime now, boolean qualifying, Long userId) {
        this.sessionEnd = now;
        if (qualifying) {
            this.qualified = true;
        }
        if (this.userId == null && userId != null) {
            this.userId = userId;
        }
    }
}
