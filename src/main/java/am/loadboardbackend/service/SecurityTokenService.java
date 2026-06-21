package am.loadboardbackend.service;

import am.loadboardbackend.model.SecurityToken;
import am.loadboardbackend.model.SecurityToken.TokenType;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.SecurityTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityTokenService {

    private static final long EMAIL_VERIFICATION_EXPIRY_HOURS = 24;
    private static final long PASSWORD_RESET_EXPIRY_HOURS = 1;
    private static final long LOGIN_CODE_EXPIRY_MINUTES = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    private final SecurityTokenRepository securityTokenRepository;

    // ── Token creation ────────────────────────────────────────────────────────

    @Transactional
    public SecurityToken createEmailVerificationToken(User user) {
        // Invalidate any existing tokens for this user + type
        securityTokenRepository.deleteAllByUserAndTokenType(user, TokenType.EMAIL_VERIFICATION);

        SecurityToken securityToken = SecurityToken.builder()
                .token(UUID.randomUUID().toString())
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(EMAIL_VERIFICATION_EXPIRY_HOURS))
                .build();

        SecurityToken saved = securityTokenRepository.save(securityToken);
        log.info("Created EMAIL_VERIFICATION token for userId={}", user.getId());
        return saved;
    }

    @Transactional
    public SecurityToken createPasswordResetToken(User user) {
        // Invalidate any existing reset tokens for this user
        securityTokenRepository.deleteAllByUserAndTokenType(user, TokenType.PASSWORD_RESET);

        SecurityToken securityToken = SecurityToken.builder()
                .token(UUID.randomUUID().toString())
                .tokenType(TokenType.PASSWORD_RESET)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(PASSWORD_RESET_EXPIRY_HOURS))
                .build();

        SecurityToken saved = securityTokenRepository.save(securityToken);
        log.info("Created PASSWORD_RESET token for userId={}", user.getId());
        return saved;
    }

    @Transactional
    public SecurityToken createLoginCodeToken(User user) {
        securityTokenRepository.deleteAllByUserAndTokenType(user, TokenType.LOGIN_CODE);
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        SecurityToken securityToken = SecurityToken.builder()
                .token(code)
                .tokenType(TokenType.LOGIN_CODE)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(LOGIN_CODE_EXPIRY_MINUTES))
                .build();
        SecurityToken saved = securityTokenRepository.save(securityToken);
        log.info("Created LOGIN_CODE for userId={}", user.getId());
        return saved;
    }

    @Transactional
    public void consumeLoginCode(String code, User user) {
        SecurityToken token = securityTokenRepository
                .findByTokenAndTokenTypeAndUser(code, TokenType.LOGIN_CODE, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code"));
        if (!token.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    token.isExpired() ? "Code has expired" : "Code has already been used");
        }
        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        securityTokenRepository.save(token);
        log.info("Consumed LOGIN_CODE for userId={}", user.getId());
    }

    // ── Token validation ──────────────────────────────────────────────────────

    /**
     * Validates and returns the token entity without consuming it.
     * Throws 400 if not found, expired, or already used.
     */
    public SecurityToken validateToken(String rawToken, TokenType expectedType) {
        SecurityToken securityToken = securityTokenRepository
                .findByTokenAndTokenType(rawToken, expectedType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (!securityToken.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    securityToken.isExpired() ? "Token has expired" : "Token has already been used");
        }
        return securityToken;
    }

    /**
     * Validates and immediately marks the token as used (consumes it).
     */
    @Transactional
    public SecurityToken consumeToken(String rawToken, TokenType expectedType) {
        SecurityToken securityToken = validateToken(rawToken, expectedType);
        securityToken.setUsed(true);
        securityToken.setUsedAt(LocalDateTime.now());
        SecurityToken consumed = securityTokenRepository.save(securityToken);
        log.info("Consumed {} token id={} for userId={}", expectedType, consumed.getId(), consumed.getUser().getId());
        return consumed;
    }

    // ── Scheduled cleanup ─────────────────────────────────────────────────────

    /** Runs every day at 03:00 AM to prune expired tokens from the DB. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        securityTokenRepository.deleteAllExpired(LocalDateTime.now());
        log.info("Purged expired security tokens");
    }
}
