package com.example.nutriuniv.domain.admin.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.admin.dto.*;
import com.example.nutriuniv.domain.admin.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin - Stats", description = "관리자 통계 / 대시보드 API")
@RestController
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    // GET /admin/dashboard
    @Operation(summary = "대시보드 통계 조회",
            description = "전체 상품 수, 카테고리 수(depth별), 평균 영양점수, 쿠팡 연동 현황을 반환합니다.")
    @GetMapping("/admin/dashboard")
    public ResponseEntity<CommonResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(CommonResponse.success(adminStatsService.getDashboard()));
    }

    // GET /admin/stats/coupang
    @Operation(summary = "쿠팡 클릭/수익 통계",
            description = "날짜 형식: yyyyMMdd. 최대 조회 범위 30일.")
    @GetMapping("/admin/stats/coupang")
    public ResponseEntity<CommonResponse<List<AdminCoupangStatResponse>>> getCoupangStats(
            @Parameter(description = "시작일 (yyyyMMdd)") @RequestParam String startDate,
            @Parameter(description = "종료일 (yyyyMMdd)") @RequestParam String endDate) {

        return ResponseEntity.ok(CommonResponse.success(
                adminStatsService.getCoupangStats(startDate, endDate)));
    }

    // GET /admin/stats/views
    @Operation(summary = "상세페이지 조회 통계",
            description = "날짜 형식: yyyyMMdd. 최대 조회 범위 30일.")
    @GetMapping("/admin/stats/views")
    public ResponseEntity<CommonResponse<List<AdminViewStatResponse>>> getViewStats(
            @Parameter(description = "시작일 (yyyyMMdd)") @RequestParam String startDate,
            @Parameter(description = "종료일 (yyyyMMdd)") @RequestParam String endDate) {

        return ResponseEntity.ok(CommonResponse.success(
                adminStatsService.getViewStats(startDate, endDate)));
    }

    // GET /admin/stats/searches
    @Operation(summary = "검색량 통계",
            description = "날짜 형식: yyyyMMdd. 최대 조회 범위 30일.")
    @GetMapping("/admin/stats/searches")
    public ResponseEntity<CommonResponse<List<AdminSearchStatResponse>>> getSearchStats(
            @Parameter(description = "시작일 (yyyyMMdd)") @RequestParam String startDate,
            @Parameter(description = "종료일 (yyyyMMdd)") @RequestParam String endDate) {

        return ResponseEntity.ok(CommonResponse.success(
                adminStatsService.getSearchStats(startDate, endDate)));
    }

    // GET /admin/stats/retention
    @Operation(summary = "재방문 W2 합격 바 (return_visit)",
            description = "cohort(warm/cold)별 W2 리텐션. W1(Day0–6) 유효 방문 유저 중 " +
                    "4주 창(Day7–27) 내 유기적 유효 재방문 비율. " +
                    "warm ≥20% PASS / <7% FAIL / 7–20% S4. (측정 창에 리마인드 미발송 전제)")
    @GetMapping("/admin/stats/retention")
    public ResponseEntity<CommonResponse<List<AdminRetentionResponse>>> getRetention() {
        return ResponseEntity.ok(CommonResponse.success(adminStatsService.getRetentionW2()));
    }
}
