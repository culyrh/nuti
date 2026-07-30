package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.dto.DailySearchStat;
import com.example.nutriuniv.domain.logging.entity.SearchLog;
import com.example.nutriuniv.domain.search.dto.RecentKeywordResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    // 인기 검색어 집계: 기간 내 키워드별 검색 횟수 내림차순 (Pageable로 상위 N개 제한)
    // 반환: [keyword(String), count(Number)]
    @Query(value = """
            SELECT keyword AS keyword,
                   COUNT(*) AS cnt
            FROM search_logs
            WHERE searched_at >= :start AND searched_at < :end
            GROUP BY keyword
            ORDER BY cnt DESC, keyword ASC
            """, nativeQuery = true)
    List<Object[]> findKeywordCounts(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     Pageable pageable);

    // 관리자 검색 통계 (일별 검색수 / 고유 키워드 수)
    // native query + Object[] 대신 JPQL 생성자 표현식을 사용한다.
    @Query("""
            SELECT new com.example.nutriuniv.domain.logging.dto.DailySearchStat(
                       CAST(s.searchedAt AS LocalDate),
                       COUNT(s),
                       COUNT(DISTINCT s.keyword))
            FROM SearchLog s
            WHERE s.searchedAt >= :start AND s.searchedAt < :end
            GROUP BY CAST(s.searchedAt AS LocalDate)
            ORDER BY CAST(s.searchedAt AS LocalDate)
            """)
    List<DailySearchStat> findDailyStats(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    // keyword 기준으로 중복 제거 후, 가장 최근 searched_at 기준 내림차순 정렬, 최대 10개
    @Query("""
            SELECT new com.example.nutriuniv.domain.search.dto.RecentKeywordResponse(
                s.keyword, MAX(s.searchedAt)
            )
            FROM SearchLog s
            WHERE s.userId = :userId
            GROUP BY s.keyword
            ORDER BY MAX(s.searchedAt) DESC
            """)
    List<RecentKeywordResponse> findRecentKeywords(@Param("userId") Long userId, Pageable pageable);
}