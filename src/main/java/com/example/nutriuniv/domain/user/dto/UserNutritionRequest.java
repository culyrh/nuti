package com.example.nutriuniv.domain.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class UserNutritionRequest {

    @NotNull(message = "올바른 정보를 입력해 주세요.")
    @DecimalMin(value = "50.0", message = "올바른 정보를 입력해 주세요.")
    @DecimalMax(value = "250.0", message = "올바른 정보를 입력해 주세요.")
    private BigDecimal height;

    @NotNull(message = "올바른 정보를 입력해 주세요.")
    @DecimalMin(value = "10.0", message = "올바른 정보를 입력해 주세요.")
    @DecimalMax(value = "200.0", message = "올바른 정보를 입력해 주세요.")
    private BigDecimal weight;

    // 선택 항목 - null 허용, 값이 있을 때만 범위 검증
    @DecimalMin(value = "1.0", message = "올바른 정보를 입력해 주세요.")
    @DecimalMax(value = "70.0", message = "올바른 정보를 입력해 주세요.")
    private BigDecimal bodyFatRate;

    @DecimalMin(value = "5.0", message = "올바른 정보를 입력해 주세요.")
    @DecimalMax(value = "100.0", message = "올바른 정보를 입력해 주세요.")
    private BigDecimal skeletalMuscleMass;

    private String dietPurpose;
    private String activityType;
    private int weeklyExerciseCount;
    private String exerciseIntensity;
    private Integer dailyMealCount;
    private Integer dailySnackCount;
}