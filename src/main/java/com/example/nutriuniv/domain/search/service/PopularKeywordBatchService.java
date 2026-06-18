package com.example.nutriuniv.domain.search.service;

import com.example.nutriuniv.domain.logging.repository.SearchLogRepository;
import com.example.nutriuniv.domain.search.entity.PopularKeyword;
import com.example.nutriuniv.domain.search.entity.PopularKeywordHistory;
import com.example.nutriuniv.domain.search.repository.PopularKeywordHistoryRepository;
import com.example.nutriuniv.domain.search.repository.PopularKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 인기 검색어 집계 배치.
 *
 * <ul>
 *   <li>시간별: 오늘 00:00~현재 검색 로그로 popular_keywords(오늘 실시간 순위) 갱신</li>
 *   <li>일별: 전날 00:00~23:59:59 검색 로그로 popular_keyword_history(전일 확정 순위) 저장</li>
 * </ul>
 *
 * 조회 시 오늘 순위 ↔ 전일 스냅샷을 비교해 순위 변동(UP/DOWN/SAME/NEW)을 산출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularKeywordBatchService {

    private static final int TOP_N = 10;

    private final SearchLogRepository searchLogRepository;
    private final PopularKeywordRepository popularKeywordRepository;
    private final PopularKeywordHistoryRepository popularKeywordHistoryRepository;

    // 앱 기동 직후 1회 — 다음 정각 전까지 비어있지 않게 시드
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initOnStartup() {
        try {
            refreshTodayRanking();
        } catch (Exception e) {
            log.error("[PopularKeyword] 기동 시 초기 집계 실패: {}", e.getMessage());
        }
    }

    // 매시 정각 — 오늘 실시간 순위 갱신
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void refreshTodayRanking() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Object[]> rows = searchLogRepository.findKeywordCounts(start, end, PageRequest.of(0, TOP_N));

        List<PopularKeyword> ranking = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            String keyword = (String) row[0];
            int count = ((Number) row[1]).intValue();
            ranking.add(PopularKeyword.create(keyword, count, rank++));
        }

        // 현재 순위 통째 교체
        popularKeywordRepository.deleteAllInBulk();
        popularKeywordRepository.saveAll(ranking);
        log.info("[PopularKeyword] 오늘 순위 갱신 완료 - {}건", ranking.size());
    }

    // 매일 00:05 — 전날 확정 순위 스냅샷
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void snapshotYesterdayRanking() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Object[]> rows = searchLogRepository.findKeywordCounts(start, end, PageRequest.of(0, TOP_N));

        List<PopularKeywordHistory> snapshot = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            String keyword = (String) row[0];
            int count = ((Number) row[1]).intValue();
            snapshot.add(PopularKeywordHistory.create(yesterday, keyword, count, rank++));
        }

        // 멱등성: 같은 날짜 스냅샷이 있으면 지우고 다시 저장
        popularKeywordHistoryRepository.deleteBySnapshotDate(yesterday);
        popularKeywordHistoryRepository.saveAll(snapshot);
        log.info("[PopularKeyword] 전일({}) 스냅샷 저장 완료 - {}건", yesterday, snapshot.size());
    }
}
