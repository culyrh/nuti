package com.example.nutriuniv.domain.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewPageResponse {

    private long total;
    private Double avgScoreOverall;
    private List<ReviewItem> items;

    @Getter
    @Builder
    public static class ReviewItem {
        private Long reviewId;
        private String nickname;
        private int scoreOverall;
        private String content;
        private List<String> images;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}