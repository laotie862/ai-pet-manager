package com.example.demo.auth;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.JwtService;
import com.example.demo.common.security.SecurityProperties;
import com.example.demo.common.security.TokenBlacklistService;
import com.example.demo.user.UserAccount;
import com.example.demo.user.UserCreateCommand;
import com.example.demo.user.UserRepository;
import com.example.demo.user.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SecurityProperties securityProperties,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Email already exists");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Phone already exists");
        }

        try {
            Long userId = userRepository.create(new UserCreateCommand(
                    request.email(),
                    request.phone(),
                    passwordEncoder.encode(request.password()),
                    request.nickname(),
                    "USER",
                    "ACTIVE"
            ));
            UserAccount user = userRepository.findById(userId).orElseThrow();
            return issueTokens(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "User already exists");
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByAccount(request.account())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid account or password"));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid account or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String tokenHash = tokenHash(request.refreshToken());
        RefreshTokenRecord refreshToken = refreshTokenRepository.findByHash(tokenHash)
                .filter(RefreshTokenRecord::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        UserAccount user = userRepository.findById(refreshToken.userId())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        refreshTokenRepository.revoke(tokenHash);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String authorizationHeader, LogoutRequest request) {
        String accessToken = resolveBearerToken(authorizationHeader);
        if (accessToken != null) {
            try {
                tokenBlacklistService.blacklist(accessToken, jwtService.expiresAt(accessToken));
            } catch (RuntimeException ignored) {
                // Invalid access tokens are already useless; logout should stay idempotent.
            }
        }
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            refreshTokenRepository.revoke(tokenHash(request.refreshToken()));
        }
    }

    private TokenResponse issueTokens(UserAccount user) {
        String accessToken = jwtService.createAccessToken(user.id(), user.role());
        String refreshToken = createRefreshToken();
        OffsetDateTime refreshExpiresAt = OffsetDateTime.now(ZoneOffset.UTC)
                .plusDays(securityProperties.getRefreshTokenDays());
        refreshTokenRepository.create(user.id(), tokenHash(refreshToken), refreshExpiresAt);

        return new TokenResponse(
                "Bearer",
                accessToken,
                securityProperties.getAccessTokenMinutes() * 60,
                refreshToken,
                UserResponse.from(user)
        );
    }

    private String createRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String tokenHash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash token", exception);
        }
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }
}
