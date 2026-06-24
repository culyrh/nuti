package com.example.nutriuniv.domain.review.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.review.dto.ReviewCreateResponse;
import com.example.nutriuniv.domain.review.dto.ReviewPageResponse;
import com.example.nutriuniv.domain.review.dto.ReviewRequest;
import com.example.nutriuniv.domain.review.dto.ReviewUpdateRequest;
import com.example.nutriuniv.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review", description = "리뷰 API")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // GET /reviews/{productId}
    @Operation(summary = "상품 리뷰 목록 조회",
            description = "상품 ID로 활성 리뷰 목록과 평균 점수를 조회합니다. 비로그인도 접근 가능합니다.")
    @GetMapping("/reviews/{productId}")
    public ResponseEntity<CommonResponse<ReviewPageResponse>> getReviews(
            @Parameter(description = "상품 ID") @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) { // 비로그인 시 null

        Long userId = (principal != null) ? principal.getId() : null;

        return ResponseEntity.ok(CommonResponse.success(
                reviewService.getReviews(productId, page, size, userId)));
    }

    // POST /reviews/{productId}
    @Operation(summary = "리뷰 작성",
            description = "상품에 리뷰를 작성합니다. 동일 상품에 중복 작성 시 409를 반환합니다.")
    @PostMapping("/reviews/{productId}")
    public ResponseEntity<CommonResponse<ReviewCreateResponse>> createReview(
            @Parameter(description = "상품 ID") @PathVariable Long productId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewRequest request) {

        return ResponseEntity.ok(CommonResponse.success(
                reviewService.createReview(productId, principal.getId(), request)));
    }

    // PATCH /reviews/{reviewId}
    @Operation(summary = "리뷰 수정",
            description = "본인 리뷰를 수정합니다. 타인 리뷰 수정 시 403을 반환합니다.")
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<Void>> updateReview(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewUpdateRequest request) {

        reviewService.updateReview(reviewId, principal.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // DELETE /reviews/{reviewId}
    @Operation(summary = "리뷰 삭제",
            description = "본인 리뷰를 소프트 딜리트(is_active=false) 처리합니다. 타인 리뷰 삭제 시 403을 반환합니다.")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<Void>> deleteReview(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {

        reviewService.deleteReview(reviewId, principal.getId());
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}