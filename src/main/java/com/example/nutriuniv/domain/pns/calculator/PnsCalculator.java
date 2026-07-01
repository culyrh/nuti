package com.example.nutriuniv.domain.pns.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PNS 점수 산출 — 기능명세서 v4.1
 *
 * 입력 단위 : 100g당 기준
 * goal      : "diet" | "bulk"
 *
 * 파이프라인
 *   Step1. B (유익)  = protein + fiber 기여
 *   Step2. L (제한)  = 나쁜 영양소 5개 합산
 *   Step3. core      = B - L
 *   Step4. ED        = 에너지밀도 (goal별 M 기준)
 *   Step5. raw       = core - ED (diet) / core + ED (bulk)
 *   Step6. score     = raw → 0~100 정규화 (고정 앵커)
 *          health    = core → 0~100 정규화 (고정 앵커)
 */
public final class PnsCalculator {

    // ── 정규화 앵커 (1,924개 채점셋 P1/P99 동결) ──────────────────────────────
    private static final double DIET_RAW_LO   = -1.76;
    private static final double DIET_RAW_HI   =  1.35;
    private static final double BULK_RAW_LO   = -0.60;
    private static final double BULK_RAW_HI   =  2.69;
    private static final double HEALTH_RAW_LO = -1.07;
    private static final double HEALTH_RAW_HI =  1.81;

    // ── EER 범위 끝점 (diet M 계산용) ─────────────────────────────────────────
    private static final double EER_MIN = 1500.0;
    private static final double EER_MAX = 2600.0;

    // ── 유익 만점선 ────────────────────────────────────────────────────────────
    private static final double PROTEIN_MAX = 11.0;  // g/100g
    private static final double FIBER_MAX   =  6.0;  // g/100g

    // ── 제한 감점 구간 [시작, 최대] ───────────────────────────────────────────
    private static final double SUGAR_LO   =    5.0;  private static final double SUGAR_HI   = 100.0;
    private static final double SATFAT_LO  =    1.5;  private static final double SATFAT_HI  =  15.0;
    private static final double SODIUM_LO  =  120.0;  private static final double SODIUM_HI  = 2000.0;
    private static final double CHOL_LO    =   20.0;  private static final double CHOL_HI    =  300.0;
    private static final double TRANS_LO   =    0.2;  private static final double TRANS_HI   =   2.0;

    // ── 에너지밀도 ─────────────────────────────────────────────────────────────
    private static final double ED_ZERO = 40.0;   // kcal/100g 이하 → ED=0
    private static final double M_MIN   = 400.0;
    private static final double M_MAX   = 900.0;
    private static final double BULK_M  = 400.0;  // bulk 고정

    private PnsCalculator() {}

    // ── 반환 타입 ──────────────────────────────────────────────────────────────

    public static class Result {
        public final double score;        // 0~100, 소수 1자리
        public final String grade;        // A~E
        public final double healthScore;  // goal 무관 건강점수
        public final Breakdown breakdown; // 중간값 (디버깅용)

        Result(double score, String grade, double healthScore, Breakdown breakdown) {
            this.score       = score;
            this.grade       = grade;
            this.healthScore = healthScore;
            this.breakdown   = breakdown;
        }
    }

    public static class Breakdown {
        public final double B;
        public final double L;
        public final double core;
        public final double M;
        public final double ED;
        public final double raw;

        Breakdown(double B, double L, double core, double M, double ED, double raw) {
            this.B    = B;
            this.L    = L;
            this.core = core;
            this.M    = M;
            this.ED   = ED;
            this.raw  = raw;
        }
    }

    // ── 메인 진입점 ────────────────────────────────────────────────────────────

    /**
     * @param goal     "diet" 또는 "bulk"
     * @param eer      사용자 EER (kcal/day). 객관 모드면 2000 고정으로 넘길 것
     * @param calories kcal/100g
     * @param protein  g/100g
     * @param fiber    g/100g  (dietary_fiber)
     * @param sugar    g/100g
     * @param satFat   g/100g  (saturated_fat)
     * @param transFat g/100g
     * @param sodium   mg/100g
     * @param chol     mg/100g (cholesterol)
     */
    public static Result calculate(String goal, double eer,
                                   BigDecimal calories, BigDecimal protein, BigDecimal fiber,
                                   BigDecimal sugar,    BigDecimal satFat,  BigDecimal transFat,
                                   BigDecimal sodium,   BigDecimal chol) {

        boolean isDiet = !"bulk".equalsIgnoreCase(goal);

        double cal  = num(calories);
        double prot = num(protein);
        double fib  = num(fiber);
        double sug  = num(sugar);
        double sf   = num(satFat);
        double tf   = num(transFat);
        double na   = num(sodium);
        double cho  = num(chol);

        // Step 1 — B (유익)
        double B = Math.min(prot / PROTEIN_MAX, 1.0)
                + Math.min(fib  / FIBER_MAX,   1.0);

        // Step 2 — L (제한)
        double L = clipRange(sug, SUGAR_LO,  SUGAR_HI)
                + clipRange(sf,  SATFAT_LO, SATFAT_HI)
                + clipRange(na,  SODIUM_LO, SODIUM_HI)
                + clipRange(cho, CHOL_LO,   CHOL_HI)
                + clipRange(tf,  TRANS_LO,  TRANS_HI);

        // Step 3 — core
        double core = B - L;

        // Step 4 — ED
        double M;
        if (isDiet) {
            M = clamp(400.0 + 500.0 * (eer - EER_MIN) / (EER_MAX - EER_MIN), M_MIN, M_MAX);
        } else {
            M = BULK_M;
        }
        double ED = cal <= ED_ZERO ? 0.0 : clip((cal - ED_ZERO) / (M - ED_ZERO));

        // Step 5 — raw
        double raw = isDiet ? core - ED : core + ED;

        // Step 6 — score 0~100 정규화
        double rawLo = isDiet ? DIET_RAW_LO : BULK_RAW_LO;
        double rawHi = isDiet ? DIET_RAW_HI : BULK_RAW_HI;
        double score = round1(clamp((raw - rawLo) / (rawHi - rawLo) * 100.0, 0.0, 100.0));

        // 건강점수 (goal 무관)
        double health = round1(clamp(
                (core - HEALTH_RAW_LO) / (HEALTH_RAW_HI - HEALTH_RAW_LO) * 100.0, 0.0, 100.0));

        String     grade = toGrade(score, goal);
        Breakdown  bd    = new Breakdown(B, L, core, M, ED, raw);

        return new Result(score, grade, health, bd);
    }

    // ── 등급 컷오프 §6 ────────────────────────────────────────────────────────

    public static String toGrade(double score, String goal) {
        if ("bulk".equalsIgnoreCase(goal)) {
            if (score >= 58.0) return "A";
            if (score >= 45.4) return "B";
            if (score >= 36.3) return "C";
            if (score >= 24.7) return "D";
            return "E";
        }
        // diet (기본)
        if (score >= 74.3) return "A";
        if (score >= 65.2) return "B";
        if (score >= 57.8) return "C";
        if (score >= 45.5) return "D";
        return "E";
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    /** null → 0.0 */
    private static double num(BigDecimal v) {
        if (v == null) return 0.0;
        double d = v.doubleValue();
        return (Double.isNaN(d) || Double.isInfinite(d)) ? 0.0 : d;
    }

    /** clip((x - lo) / (hi - lo)) → [0, 1] */
    private static double clipRange(double x, double lo, double hi) {
        return clip((x - lo) / (hi - lo));
    }

    /** 0~1 클램프 */
    private static double clip(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }

    /** min~max 클램프 */
    private static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }

    /** 소수 1자리 반올림 */
    private static double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}