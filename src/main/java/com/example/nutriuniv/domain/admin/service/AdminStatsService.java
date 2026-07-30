package com.example.nutriuniv.domain.admin.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.admin.dto.*;
import com.example.nutriuniv.domain.category.repository.CategoryRepository;
import com.example.nutriuniv.domain.coupang.repository.CoupangDailyReportRepository;
import com.example.nutriuniv.domain.coupang.repository.CoupangLinkRepository;
import com.example.nutriuniv.domain.logging.repository.ProductViewLogRepository;
import com.example.nutriuniv.domain.logging.repository.SearchLogRepository;
import com.example.nutriuniv.domain.logging.repository.VisitSessionRepository;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CoupangLinkRepository coupangLinkRepository;
    private final CoupangDailyReportRepository coupangDailyReportRepository;
    private final ProductViewLogRepository productViewLogRepository;
    private final SearchLogRepository searchLogRepository;
    private final VisitSessionRepository visitSessionRepository;

    // ── GET /admin/dashboard ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalProducts = productRepository.countByIsActiveTrue();

        AdminDashboardResponse.CategoryStats categoryStats = AdminDashboardResponse.CategoryStats.builder()
                .depth1(categoryRepository.countByDepthAndIsActiveTrue(1))
                .depth2(categoryRepository.countByDepthAndIsActiveTrue(2))
                .depth3(categoryRepository.countByDepthAndIsActiveTrue(3))
                .build();

        long totalLinks = coupangLinkRepository.count();
        AdminDashboardResponse.CoupangLinkStats coupangLinkStats = AdminDashboardResponse.CoupangLinkStats.builder()
                .total(totalLinks)
                .linked(coupangLinkRepository.countByLinkStatus("LINKED"))
                .unlinked(coupangLinkRepository.countByLinkStatus("UNLINKED"))
                .failed(coupangLinkRepository.countByLinkStatus("FAILED"))
                .build();

        return AdminDashboardResponse.builder()
                .totalProducts(totalProducts)
                .totalCategories(categoryStats)
                .avgNutritionScore(productRepository.findAvgNutritionScore())
                .lastUpdatedAt(productRepository.findLatestUpdatedAt())
                .coupangLinkStatus(coupangLinkStats)
                .build();
    }

    // ── GET /admin/stats/coupang ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminCoupangStatResponse> getCoupangStats(String startDateStr, String endDateStr) {
        LocalDate[] range = validateDateRange(startDateStr, endDateStr);

        return coupangDailyReportRepository
                .findByDateBetweenOrderByDateAsc(range[0], range[1])
                .stream()
                .map(AdminCoupangStatResponse::from)
                .collect(Collectors.toList());
    }

    // ── GET /admin/stats/views ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminViewStatResponse> getViewStats(String startDateStr, String endDateStr) {
        LocalDate[] range = validateDateRange(startDateStr, endDateStr);
        LocalDateTime start = range[0].atStartOfDay();
        LocalDateTime end = range[1].plusDays(1).atStartOfDay();

        return productViewLogRepository.findDailyStats(start, end).stream()
                .map(row -> AdminViewStatResponse.builder()
                        .date(row.date())
                        .viewCount(row.viewCount())
                        .uniqueProductCount(row.uniqueProductCount())
                        .build())
                .collect(Collectors.toList());
    }

    // ── GET /admin/stats/searches ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminSearchStatResponse> getSearchStats(String startDateStr, String endDateStr) {
        LocalDate[] range = validateDateRange(startDateStr, endDateStr);
        LocalDateTime start = range[0].atStartOfDay();
        LocalDateTime end = range[1].plusDays(1).atStartOfDay();

        return searchLogRepository.findDailyStats(start, end).stream()
                .map(row -> AdminSearchStatResponse.builder()
                        .date(row.date())
                        .searchCount(row.searchCount())
                        .uniqueKeywordCount(row.uniqueKeywordCount())
                        .build())
                .collect(Collectors.toList());
    }

    // ── GET /admin/stats/retention ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminRetentionResponse> getRetentionW2() {
        return visitSessionRepository.findRetentionW2().stream()
                .map(row -> {
                    String cohort = (String) row[0];
                    long w1Users = ((Number) row[1]).longValue();
                    long retainedUsers = ((Number) row[2]).longValue();
                    double rate = w1Users == 0 ? 0.0 : (double) retainedUsers / w1Users;
                    return AdminRetentionResponse.builder()
                            .cohort(cohort)
                            .w1Users(w1Users)
                            .retainedUsers(retainedUsers)
                            .retentionRate(rate)
                            .verdict(resolveVerdict(cohort, rate))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // warm 기준: ≥20% PASS / <7% FAIL / 7–20% S4. (cold는 참고용으로 동일 기준 표기)
    private String resolveVerdict(String cohort, double rate) {
        if (rate >= 0.20) {
            return "PASS";
        }
        if (rate < 0.07) {
            return "FAIL";
        }
        return "S4";
    }

    // ── 공통 날짜 검증 ─────────────────────────────────────────────────────────────

    private LocalDate[] validateDateRange(String startDateStr, String endDateStr) {
        LocalDate start = parseDate(startDateStr);
        LocalDate end = parseDate(endDateStr);

        if (start.isAfter(end)) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "시작일이 종료일보다 클 수 없습니다.");
        }
        if (ChronoUnit.DAYS.between(start, end) > 30) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "조회 기간은 최대 30일입니다.");
        }
        return new LocalDate[]{start, end};
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "날짜 형식이 올바르지 않습니다. (yyyyMMdd)");
        }
    }
}