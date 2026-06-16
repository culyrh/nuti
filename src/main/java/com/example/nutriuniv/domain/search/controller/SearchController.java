package com.example.nutriuniv.domain.search.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.search.dto.PopularKeywordResponse;
import com.example.nutriuniv.domain.search.dto.RecentKeywordResponse;
import com.example.nutriuniv.domain.search.dto.RecommendedKeywordResponse;
import com.example.nutriuniv.domain.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Search", description = "검색 API")
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // GET /search/keywords/popular
    @Operation(summary = "인기 검색어 조회",
            description = "오늘 검색 횟수 기준 인기 검색어를 rank 순으로 최대 10개 반환합니다. " +
                    "각 항목에는 전일 대비 변동(change: UP/DOWN/SAME/NEW)과 previousRank/rankDelta가 포함됩니다. " +
                    "표시 방법은 클라이언트가 결정합니다.")
    @GetMapping("/search/keywords/popular")
    public ResponseEntity<CommonResponse<List<PopularKeywordResponse>>> getPopularKeywords() {
        return ResponseEntity.ok(CommonResponse.success(searchService.getPopularKeywords()));
    }

    // GET /search/keywords/recent
    @Operation(summary = "최근 검색어 조회",
            description = "로그인한 사용자의 최근 검색어를 최대 10개 반환합니다. " +
                    "동일 키워드는 가장 최근 검색 시각으로 중복 제거됩니다. " +
                    "비로그인 시 401을 반환합니다. (비로그인 최근 검색어는 클라이언트 localStorage로 관리)")
    @GetMapping("/search/keywords/recent")
    public ResponseEntity<CommonResponse<List<RecentKeywordResponse>>> getRecentKeywords(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(CommonResponse.success(searchService.getRecentKeywords(userId)));
    }

    // GET /search/keywords/recommended
    @Operation(summary = "추천 검색어 조회",
            description = "로그인한 사용자에게 추천 엔진(CF/CONTENT/POPULAR)이 내려주는 제품명을 최대 10개 반환합니다. " +
                    "비로그인 시 data가 null로 내려가, 추천 없음을 명확히 구분합니다.")
    @GetMapping("/search/keywords/recommended")
    public ResponseEntity<CommonResponse<List<RecommendedKeywordResponse>>> getRecommendedKeywords(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(CommonResponse.success(searchService.getRecommendedKeywords(userId)));
    }
}
