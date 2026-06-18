package com.example.nutriuniv.domain.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RegisterRequest {
    private String provider;
    private String oauthId;

    @NotBlank(message = "올바른 정보를 입력해 주세요.")
    @Size(min = 2, message = "올바른 정보를 입력해 주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "올바른 정보를 입력해 주세요.")
    private String name;

    @NotBlank(message = "올바른 정보를 입력해 주세요.")
    @Email(
            message = "올바른 정보를 입력해 주세요.",
            regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    )
    private String email;

    @NotBlank(message = "올바른 정보를 입력해 주세요.")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "올바른 정보를 입력해 주세요.")
    private String gender;

    @NotNull(message = "올바른 정보를 입력해 주세요.")
    @PastOrPresent(message = "올바른 정보를 입력해 주세요.")
    private LocalDate birthDate;

    // ── 동의 항목 ─────────────────────────────────────────────────────────────────
    @AssertTrue(message = "개인정보 수집·이용에 동의해야 합니다.")
    private boolean personalInfoAgreed;  // ① 개인정보 수집·이용 동의 (필수)

    private boolean healthInfoAgreed;    // ② 건강정보 수집·이용 동의 (선택)

    @AssertTrue(message = "만 14세 이상 확인이 필요합니다.")
    private boolean ageConfirmed;        // ③ 만 14세 이상 확인 (필수)
}