package com.example.nutriuniv.domain.logging.entity;

import com.example.nutriuniv.domain.logging.dto.LogContext;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs",
        indexes = @Index(name = "idx_search_logs_anon", columnList = "anonymous_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "anonymous_id", length = 36)
    private String anonymousId;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "cohort", length = 10)
    private String cohort;

    @Column(nullable = false, length = 30)
    private String keyword;

    @Column(name = "result_count", nullable = false)
    private int resultCount = 0;

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    public static SearchLog create(LogContext ctx, String keyword, int resultCount) {
        SearchLog log = new SearchLog();
        log.userId = ctx.userId();
        log.anonymousId = ctx.anonymousId();
        log.sessionId = ctx.sessionId();
        log.cohort = ctx.cohort();
        log.keyword = keyword;
        log.resultCount = resultCount;
        log.searchedAt = LocalDateTime.now();
        return log;
    }
}
