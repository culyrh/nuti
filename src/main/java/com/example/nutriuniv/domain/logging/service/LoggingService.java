package com.example.nutriuniv.domain.logging.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.logging.dto.CtaLogRequest;
import com.example.nutriuniv.domain.logging.dto.FilterLogRequest;
import com.example.nutriuniv.domain.logging.dto.LogContext;
import com.example.nutriuniv.domain.logging.dto.SearchLogRequest;
import com.example.nutriuniv.domain.logging.dto.ViewLogRequest;
import com.example.nutriuniv.domain.logging.entity.ProductCtaLog;
import com.example.nutriuniv.domain.logging.entity.ProductFilterLog;
import com.example.nutriuniv.domain.logging.entity.ProductViewLog;
import com.example.nutriuniv.domain.logging.entity.SearchLog;
import com.example.nutriuniv.domain.logging.repository.ProductCtaLogRepository;
import com.example.nutriuniv.domain.logging.repository.ProductFilterLogRepository;
import com.example.nutriuniv.domain.logging.repository.ProductViewLogRepository;
import com.example.nutriuniv.domain.logging.repository.SearchLogRepository;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {

    private static final int CTA_RATE_LIMIT_HOURS = 1;

    private final ProductViewLogRepository productViewLogRepository;
    private final ProductCtaLogRepository productCtaLogRepository;
    private final SearchLogRepository searchLogRepository;
    private final ProductFilterLogRepository productFilterLogRepository;
    private final ProductRepository productRepository;
    private final VisitTrackingService visitTrackingService;

    // ── POST /logging/view (product_detail_viewed, 유효 방문) ──────────────────────

    @Transactional
    public void logProductView(ViewLogRequest request, LogContext ctx) {
        if (request.getProductId() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "product_id는 필수입니다.");
        }

        // 유효 방문 → 세션 추적 (상품 존재 여부와 무관하게 방문 자체는 카운트)
        visitTrackingService.track(ctx, true);

        try {
            Product product = productRepository.findById(request.getProductId()).orElse(null);
            if (product == null) {
                log.warn("[ViewLog] 존재하지 않는 product_id: {}", request.getProductId());
                return;
            }
            productViewLogRepository.save(ProductViewLog.create(product, ctx));
        } catch (Exception e) {
            log.error("[ViewLog] 로그 저장 실패 - productId: {}, error: {}", request.getProductId(), e.getMessage());
        }
    }

    // ── POST /logging/cta (coupang_outbound_clicked, 구매전환 관찰) ────────────────

    @Transactional
    public void logProductCta(CtaLogRequest request, LogContext ctx) {
        if (request.getProductId() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "product_id는 필수입니다.");
        }

        // cta는 유효 방문 아님 → 세션 종료시각만 갱신
        visitTrackingService.track(ctx, false);

        try {
            Product product = productRepository.findById(request.getProductId()).orElse(null);
            if (product == null) {
                log.warn("[CtaLog] 존재하지 않는 product_id: {}", request.getProductId());
                return;
            }

            // 로그인 유저는 1시간 내 동일 상품 중복 클릭 무시
            if (ctx.userId() != null) {
                LocalDateTime threshold = LocalDateTime.now().minusHours(CTA_RATE_LIMIT_HOURS);
                boolean alreadyLogged = productCtaLogRepository
                        .existsByUserIdAndProduct_IdAndCreatedAtAfter(ctx.userId(), request.getProductId(), threshold);
                if (alreadyLogged) {
                    log.debug("[CtaLog] 중복 클릭 무시 - userId: {}, productId: {}", ctx.userId(), request.getProductId());
                    return;
                }
            }

            productCtaLogRepository.save(ProductCtaLog.create(product, ctx));
        } catch (Exception e) {
            log.error("[CtaLog] 로그 저장 실패 - productId: {}, error: {}", request.getProductId(), e.getMessage());
        }
    }

    // ── POST /logging/search (search_performed, 유효 방문) ─────────────────────────

    @Transactional
    public void logSearch(SearchLogRequest request, LogContext ctx) {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "keyword는 필수입니다.");
        }

        // 유효 방문 → 세션 추적
        visitTrackingService.track(ctx, true);

        try {
            searchLogRepository.save(SearchLog.create(ctx, request.getKeyword().trim(), request.getResultCount()));
        } catch (Exception e) {
            log.error("[SearchLog] 로그 저장 실패 - keyword: {}, error: {}", request.getKeyword(), e.getMessage());
        }
    }

    // ── POST /logging/filter (compare_or_filter_used, 핵심 행동) ───────────────────

    @Transactional
    public void logFilter(FilterLogRequest request, LogContext ctx) {
        if (request.getFilterType() == null || request.getFilterType().isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "filter_type은 필수입니다.");
        }

        // filter는 유효 방문 아님 → 세션 종료시각만 갱신
        visitTrackingService.track(ctx, false);

        try {
            productFilterLogRepository.save(
                    ProductFilterLog.create(ctx, request.getFilterType().trim(), request.getFilterValue()));
        } catch (Exception e) {
            log.error("[FilterLog] 로그 저장 실패 - filterType: {}, error: {}",
                    request.getFilterType(), e.getMessage());
        }
    }
}
