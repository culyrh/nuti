package com.example.nutriuniv.domain.auth.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.common.security.JwtService;
import com.example.nutriuniv.domain.auth.client.OAuthClient;
import com.example.nutriuniv.domain.auth.client.OAuthClientFactory;
import com.example.nutriuniv.domain.auth.client.OAuthUserInfo;
import com.example.nutriuniv.domain.auth.dto.OAuthLoginRequest;
import com.example.nutriuniv.domain.auth.dto.RefreshRequest;
import com.example.nutriuniv.domain.auth.dto.RegisterRequest;
import com.example.nutriuniv.domain.auth.dto.TokenResponse;
import com.example.nutriuniv.domain.auth.entity.AuthToken;
import com.example.nutriuniv.domain.auth.repository.AuthTokenRepository;
import com.example.nutriuniv.domain.user.entity.User;
import com.example.nutriuniv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final JwtService jwtService;
    private final OAuthClientFactory oAuthClientFactory; // GoogleOAuthClient 직접 의존 제거

    // ── POST /auth/oauth ──────────────────────────────────────────────────────────
    // provider에 맞는 OAuthClient를 팩토리에서 꺼내 처리.
    // 신규회원이면 email + oauthId 반환 (토큰 미발급)
    // 기존회원이면 바로 토큰 발급

    @Transactional
    public TokenResponse login(OAuthLoginRequest request) {

        OAuthClient client = oAuthClientFactory.getClient(request.getProvider());
        String accessToken = client.getAccessToken(request.getCode());
        OAuthUserInfo userInfo = client.getUserInfo(accessToken);

        Optional<User> existing = userRepository.findByOauthProviderAndOauthId(
                request.getProvider().toLowerCase(), userInfo.getOauthId());

        // 신규회원: 토큰 발급 없이 email + oauthId만 반환
        if (existing.isEmpty()) {
            return TokenResponse.builder()
                    .isNewUser(true)
                    .email(userInfo.getEmail())
                    .oauthId(userInfo.getOauthId())
                    .build();
        }

        // 기존회원: 바로 토큰 발급
        User user = existing.get();
        return issueToken(user, false);
    }

    // ── POST /auth/register ───────────────────────────────────────────────────────
    // 신규회원 정보 입력 완료 후 호출. 유저 생성 + 토큰 발급.

    @Transactional
    public TokenResponse register(RegisterRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "이름은 필수입니다.");
        }
        if (request.getGender() == null || request.getGender().isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "성별은 필수입니다.");
        }
        if (request.getBirthDate() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "생년월일은 필수입니다.");
        }
        if (request.getProvider() == null || request.getOauthId() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "provider와 oauthId는 필수입니다.");
        }

        // 유효한 provider인지 검증 (OAuthProvider.from이 내부에서 예외 던짐)
        OAuthClientFactory.validateProvider(request.getProvider());

        String normalizedProvider = request.getProvider().toLowerCase();

        // 이미 가입된 경우 방어
        if (userRepository.findByOauthProviderAndOauthId(normalizedProvider, request.getOauthId()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 가입된 사용자입니다.");
        }

        User user = userRepository.save(User.create(
                normalizedProvider,
                request.getOauthId(),
                request.getName(),
                request.getGender(),
                request.getBirthDate(),
                request.getEmail()
        ));

        return issueToken(user, true);
    }

    // ── POST /auth/refresh ────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        AuthToken authToken = authTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            authTokenRepository.delete(authToken);
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = authToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        authToken.rotate(newRefreshToken,
                LocalDateTime.now().plusNanos(jwtService.getRefreshExpMs() * 1_000_000));

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    // ── POST /auth/logout ─────────────────────────────────────────────────────────

    @Transactional
    public void logout(Long userId) {
        authTokenRepository.deleteByUserId(userId);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────────

    private TokenResponse issueToken(User user, boolean isNewUser) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        authTokenRepository.deleteByUserId(user.getId());
        authTokenRepository.save(AuthToken.create(
                user, refreshToken, null,
                LocalDateTime.now().plusNanos(jwtService.getRefreshExpMs() * 1_000_000)
        ));

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .build();
    }
}