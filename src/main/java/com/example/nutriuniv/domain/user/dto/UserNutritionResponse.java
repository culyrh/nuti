package com.example.nutriuniv.domain.user.dto;

import com.example.nutriuniv.domain.user.entity.UserNutrition;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class UserNutritionResponse {
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bodyFatRate;
    private BigDecimal skeletalMuscleMass;
    private String dietPurpose;
    private String activityType;
    private int weeklyExerciseCount;
    private String exerciseIntensity;
    private Integer dailyMealCount;
    private Integer dailySnackCount;

    public static UserNutritionResponse from(UserNutrition n) {
        return UserNutritionResponse.builder()
                .height(n.getHeight())
                .weight(n.getWeight())
                .bodyFatRate(n.getBodyFatRate())
                .skeletalMuscleMass(n.getSkeletalMuscleMass())
                .dietPurpose(n.getDietPurpose())
                .activityType(n.getActivityType())
                .weeklyExerciseCount(n.getWeeklyExerciseCount())
                .exerciseIntensity(n.getExerciseIntensity())
                .dailyMealCount(n.getDailyMealCount())
                .dailySnackCount(n.getDailySnackCount())
                .build();
    }
}