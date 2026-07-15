package com.example.nutriuniv.domain.logging.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.logging.dto.CtaLogRequest;
import com.example.nutriuniv.domain.logging.dto.FilterLogRequest;
import com.example.nutriuniv.domain.logging.dto.ImpressionLogRequest;
import com.example.nutriuniv.domain.logging.dto.LogContext;
import com.example.nutriuniv.domain.logging.dto.SearchLogRequest;
import com.example.nutriuniv.domain.logging.dto.ViewLogRequest;
import com.example.nutriuniv.domain.logging.service.LoggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Logging", description = "로그 API")
@RestController
@RequiredArgsConstructor
public class LoggingController {

    // 클라이언트 식별 헤더 (localStorage UUID / 세션 / 코호트)
    private static final String HEADER_ANONYMOUS_ID = "X-Anonymous-Id";
    private static final String HEADER_SESSION_ID = "X-Session-Id";
    private static final String HEADER_COHORT = "X-Cohort";

    private final LoggingService loggingService;

    // POST /logging/view
    @Operation(summary = "상품 조회 로그 기록 (product_detail_viewed)",
            description = "상품 상세 조회 시 클라이언트가 호출합니다. 유효 방문으로 집계됩니다. " +
                    "X-Anonymous-Id / X-Session-Id / X-Cohort 헤더로 비로그인 재방문을 추적합니다. " +
                    "로그 저장 실패 시에도 200을 반환합니다. 비로그인 시 user_id는 null로 저장됩니다.")
    @PostMapping("/logging/view")
    public ResponseEntity<CommonResponse<Void>> logView(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ViewLogRequest request,
            HttpServletRequest httpServletRequest) {

        loggingService.logProductView(request, resolveContext(principal, httpServletRequest));
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // POST /logging/cta
    @Operation(summary = "쿠팡 CTA 클릭 로그 기록 (coupang_outbound_clicked)",
            description = "쿠팡 링크 클릭 시 클라이언트가 호출합니다. " +
                    "로그인 유저는 동일 상품 1시간 내 중복 클릭을 무시합니다. " +
                    "로그 저장 실패 시에도 200을 반환합니다. 비로그인 시 user_id는 null로 저장됩니다.")
    @PostMapping("/logging/cta")
    public ResponseEntity<CommonResponse<Void>> logCta(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CtaLogRequest request,
            HttpServletRequest httpServletRequest) {

        loggingService.logProductCta(request, resolveContext(principal, httpServletRequest));
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // POST /logging/search
    @Operation(summary = "검색 로그 기록 (search_performed)",
            description = "검색 실행 시 클라이언트가 호출합니다. 유효 방문으로 집계됩니다. " +
                    "keyword가 빈 문자열이면 400을 반환합니다. " +
                    "로그 저장 실패 시에도 200을 반환합니다. 비로그인 시 user_id는 null로 저장됩니다.")
    @PostMapping("/logging/search")
    public ResponseEntity<CommonResponse<Void>> logSearch(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SearchLogRequest request,
            HttpServletRequest httpServletRequest) {

        loggingService.logSearch(request, resolveContext(principal, httpServletRequest));
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // POST /logging/filter
    @Operation(summary = "필터/정렬 로그 기록 (compare_or_filter_used)",
            description = "필터/정렬/등급정렬 사용 시 클라이언트가 호출합니다. 핵심 행동 지표입니다. " +
                    "filter_type이 빈 문자열이면 400을 반환합니다. " +
                    "로그 저장 실패 시에도 200을 반환합니다. 비로그인 시 user_id는 null로 저장됩니다.")
    @PostMapping("/logging/filter")
    public ResponseEntity<CommonResponse<Void>> logFilter(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody FilterLogRequest request,
            HttpServletRequest httpServletRequest) {

        loggingService.logFilter(request, resolveContext(principal, httpServletRequest));
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // POST /logging/impression
    @Operation(summary = "상품 노출 로그 기록 (impression)",
            description = "목록/추천/검색 결과에 상품이 보여졌을 때 클라이언트가 호출합니다. " +
                    "한 화면에 보인 상품들을 items 배열로 배치 전송합니다(surface: LIST/RECOMMENDATION/SEARCH). " +
                    "추천 모델의 음성 샘플로 사용됩니다. 유효 방문에는 포함되지 않으며, " +
                    "세션 내 동일 상품 중복은 무시됩니다. 로그 저장 실패 시에도 200을 반환합니다.")
    @PostMapping("/logging/impression")
    public ResponseEntity<CommonResponse<Void>> logImpression(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ImpressionLogRequest request,
            HttpServletRequest httpServletRequest) {

        loggingService.logImpressions(request, resolveContext(principal, httpServletRequest));
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // ── 공통 컨텍스트/IP 해석 ───────────────────────────────────────────────────────

    private LogContext resolveContext(UserPrincipal principal, HttpServletRequest request) {
        Long userId = principal != null ? principal.getId() : null;
        String anonymousId = trimToNull(request.getHeader(HEADER_ANONYMOUS_ID));
        String sessionId = trimToNull(request.getHeader(HEADER_SESSION_ID));
        String cohort = LogContext.normalizeCohort(request.getHeader(HEADER_COHORT));
        String ipAddress = resolveClientIp(request);
        return new LogContext(anonymousId, sessionId, cohort, userId, ipAddress);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            return request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
