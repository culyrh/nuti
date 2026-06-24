package com.example.nutriuniv.domain.review.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import com.example.nutriuniv.domain.review.dto.ReviewCreateResponse;
import com.example.nutriuniv.domain.review.dto.ReviewPageResponse;
import com.example.nutriuniv.domain.review.dto.ReviewRequest;
import com.example.nutriuniv.domain.review.dto.ReviewUpdateRequest;
import com.example.nutriuniv.domain.review.entity.Review;
import com.example.nutriuniv.domain.review.entity.ReviewImage;
import com.example.nutriuniv.domain.review.repository.ReviewRepository;
import com.example.nutriuniv.domain.user.entity.User;
import com.example.nutriuniv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ── GET /reviews/{productId} ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReviewPageResponse getReviews(Long productId, int page, int size) {
        if (page < 1 || size < 1) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM, "page, size는 1 이상이어야 합니다.");
        }

        if (!productRepository.existsById(productId)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 상품입니다.");
        }

        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByProductIdAndIsActiveTrue(productId, pageable);

        Double avgOverall = reviewRepository.avgScoreOverall(productId);

        List<ReviewPageResponse.ReviewItem> items = reviewPage.getContent().stream()
                .map(r -> ReviewPageResponse.ReviewItem.builder()
                        .reviewId(r.getId())
                        .nickname(r.getUser().getNickname())
                        .scoreOverall(r.getScoreOverall())
                        .content(r.getContent())
                        .images(r.getImages().stream()
                                .map(ReviewImage::getImageUrl)
                                .toList())
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .toList();

        return ReviewPageResponse.builder()
                .total(reviewPage.getTotalElements())
                .avgScoreOverall(round(avgOverall))
                .items(items)
                .build();
    }

    // ── POST /reviews/{productId} ─────────────────────────────────────────────────

    @Transactional
    public ReviewCreateResponse createReview(Long productId, Long userId, ReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 상품입니다."));

        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 해당 상품에 리뷰를 작성했습니다.");
        }

        Review review = Review.create(user, product,
                request.getScoreOverall(),
                request.getContent());

        if (request.getImages() != null) {
            for (int i = 0; i < request.getImages().size(); i++) {
                ReviewImage img = ReviewImage.create(review, request.getImages().get(i), i);
                review.getImages().add(img);
            }
        }

        reviewRepository.save(review);

        return ReviewCreateResponse.builder()
                .reviewId(review.getId())
                .build();
    }

    // ── PATCH /reviews/{reviewId} ─────────────────────────────────────────────────

    @Transactional
    public void updateReview(Long reviewId, Long userId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findByIdAndIsActiveTrue(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 리뷰입니다."));

        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인 리뷰만 수정할 수 있습니다.");
        }

        review.update(request.getScoreOverall(), request.getContent());

        review.clearImages();
        if (request.getImages() != null) {
            for (int i = 0; i < request.getImages().size(); i++) {
                ReviewImage img = ReviewImage.create(review, request.getImages().get(i), i);
                review.getImages().add(img);
            }
        }
    }

    // ── DELETE /reviews/{reviewId} ────────────────────────────────────────────────

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findByIdAndIsActiveTrue(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 리뷰입니다."));

        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인 리뷰만 삭제할 수 있습니다.");
        }

        review.deactivate();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

    private Double round(Double value) {
        if (value == null) return null;
        return Math.round(value * 10.0) / 10.0;
    }
}