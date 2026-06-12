package com.example.nutriuniv.domain.auth.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.auth.dto.OAuthLoginRequest;
import com.example.nutriuniv.domain.auth.dto.RefreshRequest;
import com.example.nutriuniv.domain.auth.dto.RegisterRequest;
import com.example.nutriuniv.domain.auth.dto.TokenResponse;
import com.example.nutriuniv.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Operation(summary = "구글 OAuth 콜백")
    @GetMapping("/oauth/google")
    public ResponseEntity<Void> googleCallback(@RequestParam String code) {
        String redirectUrl = frontendUrl + "?code=" + code + "&provider=GOOGLE";
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @Operation(summary = "카카오 OAuth 콜백")
    @GetMapping("/oauth/kakao")
    public ResponseEntity<Void> kakaoCallback(@RequestParam String code) {
        String redirectUrl = frontendUrl + "?code=" + code + "&provider=KAKAO";
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @Operation(summary = "네이버 OAuth 콜백")
    @GetMapping("/oauth/naver")
    public ResponseEntity<Void> naverCallback(@RequestParam String code) {
        String redirectUrl = frontendUrl + "?code=" + code + "&provider=NAVER";
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @Operation(summary = "소셜 로그인",
            description = "기존회원이면 토큰 반환. 신규회원이면 newUser=true + oauthId 반환 (토큰 없음).")
    @PostMapping("/oauth")
    public ResponseEntity<CommonResponse<TokenResponse>> login(@RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(CommonResponse.success(authService.login(request)));
    }

    @Operation(summary = "회원가입",
            description = "신규회원이 첫번째 랜딩페이지에서 이름/이메일/성별/생년월일 입력 후 호출. 토큰 반환.")
    @PostMapping("/register")
    public ResponseEntity<CommonResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(CommonResponse.success(authService.register(request)));
    }

    @Operation(summary = "액세스 토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<TokenResponse>> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(CommonResponse.success(authService.refresh(request)));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}