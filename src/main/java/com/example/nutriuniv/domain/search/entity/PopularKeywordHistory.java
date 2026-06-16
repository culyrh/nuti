package com.example.nutriuniv.domain.search.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 인기 검색어 일별 순위 스냅샷.
 * 매일 자정 직후 배치가 "전날 00:00~23:59:59" 집계 결과를 snapshot_date=어제로 저장한다.
 * 인기 검색어 조회 시 오늘 순위와 비교해 전일 대비 변동(UP/DOWN/SAME/NEW)을 산출하는 기준값.
 */
@Entity
@Table(name = "popular_keyword_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_popular_keyword_history_date_keyword",
                columnNames = {"snapshot_date", "keyword"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularKeywordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false, length = 30)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private int searchCount = 0;

    @Column(name = "rank", nullable = false)
    private int rank;

    public static PopularKeywordHistory create(LocalDate snapshotDate, String keyword, int searchCount, int rank) {
        PopularKeywordHistory h = new PopularKeywordHistory();
        h.snapshotDate = snapshotDate;
        h.keyword = keyword;
        h.searchCount = searchCount;
        h.rank = rank;
        return h;
    }
}
