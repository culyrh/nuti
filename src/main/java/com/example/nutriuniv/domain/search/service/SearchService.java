package com.example.nutriuniv.domain.search.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.recommendation.dto.RecommendationResponse;
import com.example.nutriuniv.domain.recommendation.service.RecommendationService;
import com.example.nutriuniv.domain.search.dto.PopularKeywordResponse;
import com.example.nutriuniv.domain.search.dto.RecentKeywordResponse;
import com.example.nutriuniv.domain.search.dto.RecommendedKeywordResponse;
import com.example.nutriuniv.domain.search.entity.PopularKeyword;
import com.example.nutriuniv.domain.search.entity.PopularKeywordHistory;
import com.example.nutriuniv.domain.search.repository.PopularKeywordHistoryRepository;
import com.example.nutriuniv.domain.search.repository.PopularKeywordRepository;
import com.example.nutriuniv.domain.logging.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int RECOMMENDED_LIMIT = 10;

    private final PopularKeywordRepository popularKeywordRepository;
    private final PopularKeywordHistoryRepository popularKeywordHistoryRepository;
    private final SearchLogRepository searchLogRepository;
    private final RecommendationService recommendationService;

    // ── GET /search/keywords/popular ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PopularKeywordResponse> getPopularKeywords() {
        List<PopularKeyword> today = popularKeywordRepository.findTop10ByOrderByRankAsc();

        // 전일 순위 맵 (keyword → rank)
        Map<String, Integer> yesterdayRank = popularKeywordHistoryRepository
                .findBySnapshotDate(LocalDate.now().minusDays(1)).stream()
                .collect(Collectors.toMap(PopularKeywordHistory::getKeyword, PopularKeywordHistory::getRank));

        return today.stream()
                .map(pk -> toResponse(pk, yesterdayRank.get(pk.getKeyword())))
                .collect(Collectors.toList());
    }

    private PopularKeywordResponse toResponse(PopularKeyword pk, Integer previousRank) {
        String change;
        Integer rankDelta = null;

        if (previousRank == null) {
            change = "NEW";                       // 전일 순위권 밖 → 신규 진입
        } else {
            int delta = previousRank - pk.getRank();   // 양수 = 순위 상승
            rankDelta = delta;
            if (delta > 0) {
                change = "UP";
            } else if (delta < 0) {
                change = "DOWN";
            } else {
                change = "SAME";
            }
        }

        return PopularKeywordResponse.builder()
                .rank(pk.getRank())
                .keyword(pk.getKeyword())
                .previousRank(previousRank)
                .change(change)
                .rankDelta(rankDelta)
                .build();
    }

    // ── GET /search/keywords/recent ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecentKeywordResponse> getRecentKeywords(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return searchLogRepository.findRecentKeywords(userId, PageRequest.of(0, 10));
    }

    // ── GET /search/keywords/recommended ─────────────────────────────────────────

    /**
     * 추천 검색어. 로그인 유저만 추천 엔진을 태우고, 비로그인은 null을 반환해 명확히 구분한다.
     */
    @Transactional(readOnly = true)
    public List<RecommendedKeywordResponse> getRecommendedKeywords(Long userId) {
        if (userId == null) {
            return null;
        }

        RecommendationResponse recommendation = recommendationService.getRecommendations(userId);
        return recommendation.getItems().stream()
                .limit(RECOMMENDED_LIMIT)
                .map(item -> RecommendedKeywordResponse.builder()
                        .productId(item.getId())
                        .name(item.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
