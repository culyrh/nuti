package com.example.nutriuniv.domain.user.entity;

import com.example.nutriuniv.domain.pns.calculator.EerCalculator;
import com.example.nutriuniv.domain.pns.calculator.Gender;
import com.example.nutriuniv.domain.pns.calculator.PaCalculator;
import com.example.nutriuniv.domain.pns.calculator.PaLevel;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_nutrition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal height;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "body_fat_rate", precision = 5, scale = 2)
    private BigDecimal bodyFatRate;

    @Column(name = "skeletal_muscle_mass", precision = 5, scale = 2)
    private BigDecimal skeletalMuscleMass;

    @Column(name = "diet_purpose", nullable = false, length = 20)
    private String dietPurpose;

    @Column(name = "activity_type", nullable = false, length = 20)
    private String activityType;

    @Column(name = "weekly_exercise_count", nullable = false)
    private int weeklyExerciseCount;

    @Column(name = "exercise_intensity", nullable = false, length = 10)
    private String exerciseIntensity;

    @Column(name = "daily_meal_count")
    private Integer dailyMealCount;

    @Column(name = "daily_snack_count")
    private Integer dailySnackCount;

    @Column(precision = 7, scale = 2)
    private BigDecimal eer;

    @Column(name = "pa_level", length = 20)
    private String paLevel;

    @Column(name = "eer_band")
    private Integer eerBand;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserNutrition create(User user, BigDecimal height, BigDecimal weight,
                                       BigDecimal bodyFatRate, BigDecimal skeletalMuscleMass,
                                       String dietPurpose, String activityType,
                                       int weeklyExerciseCount, String exerciseIntensity,
                                       Integer dailyMealCount, Integer dailySnackCount) {
        UserNutrition n = new UserNutrition();
        n.user = user;
        n.height = height;
        n.weight = weight;
        n.bodyFatRate = bodyFatRate;
        n.skeletalMuscleMass = skeletalMuscleMass;
        n.dietPurpose = dietPurpose;
        n.activityType = activityType;
        n.weeklyExerciseCount = weeklyExerciseCount;
        n.exerciseIntensity = exerciseIntensity;
        n.dailyMealCount = dailyMealCount;
        n.dailySnackCount = dailySnackCount;
        n.recalculateEer();
        return n;
    }

    public void update(BigDecimal height, BigDecimal weight,
                       BigDecimal bodyFatRate, BigDecimal skeletalMuscleMass,
                       String dietPurpose, String activityType,
                       int weeklyExerciseCount, String exerciseIntensity,
                       Integer dailyMealCount, Integer dailySnackCount) {
        this.height = height;
        this.weight = weight;
        this.bodyFatRate = bodyFatRate;
        this.skeletalMuscleMass = skeletalMuscleMass;
        this.dietPurpose = dietPurpose;
        this.activityType = activityType;
        this.weeklyExerciseCount = weeklyExerciseCount;
        this.exerciseIntensity = exerciseIntensity;
        this.dailyMealCount = dailyMealCount;
        this.dailySnackCount = dailySnackCount;
        recalculateEer();
    }

    /** 신체정보 / 활동 정보 변경 시 EER, PA, eer_band 재계산. */
    public void recalculateEer() {
        if (user == null || user.getBirthDate() == null
                || height == null || weight == null) {
            return;
        }
        Gender gender = Gender.from(user.getGender());
        PaLevel pa = PaCalculator.calculate(activityType, weeklyExerciseCount, exerciseIntensity);
        double eerValue = EerCalculator.calculate(gender, user.getBirthDate(), height, weight, pa);
        this.eer = BigDecimal.valueOf(eerValue).setScale(2, java.math.RoundingMode.HALF_UP);
        this.paLevel = pa.name();
        this.eerBand = EerCalculator.toBand(eerValue);
    }
}