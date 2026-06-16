package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 재방문 W2 합격 바 — cohort별.
 * w1Users: W1(Day0–6)에 유효 방문한 고유 유저 수 (분모)
 * retainedUsers: 그 중 4주 창(Day7–27)에 유효 재방문한 유저 수 (분자)
 * retentionRate: retainedUsers / w1Users
 * verdict: warm ≥0.20 PASS / <0.07 FAIL / 그 사이 S4
 */
@Getter
@Builder
public class AdminRetentionResponse {
    private String cohort;
    private long w1Users;
    private long retainedUsers;
    private double retentionRate;
    private String verdict;
}
