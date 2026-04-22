package am.loadboardbackend.service;

import am.loadboardbackend.config.AppProperties;
import am.loadboardbackend.mailing.PasswordResetEmailContext;
import am.loadboardbackend.model.SecurityToken;
import am.loadboardbackend.model.SecurityToken.TokenType;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final SecurityTokenService securityTokenService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final PublicRegistrationService publicRegistrationService;

    // ── Email verification ────────────────────────────────────────────────────

    @Transactional
    public void verifyEmail(String rawToken) {
        SecurityToken token = securityTokenService.consumeToken(rawToken, TokenType.EMAIL_VERIFICATION);
        User user = token.getUser();

        if (user.isEmailVerified()) {
            log.info("Email already verified for userId={}", user.getId());
            return;
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Email verified for userId={}", user.getId());
    }

    /** Resends the verification email to a user who hasn't verified yet. */
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already verified");
        }

        publicRegistrationService.sendVerificationEmail(user);
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    /**
     * Initiates a password-reset flow. Always returns 200 even if the email
     * is not found (to prevent user-enumeration attacks).
     */
    public void initPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            SecurityToken token = securityTokenService.createPasswordResetToken(user);

            PasswordResetEmailContext ctx = new PasswordResetEmailContext();
            ctx.init(user);
            ctx.setFrom(appProperties.getMail().getFrom());
            ctx.setToken(token.getToken());
            ctx.buildResetUrl(appProperties.getFrontend().getBaseUrl());

            emailService.sendEmail(ctx);
            log.info("Password-reset email dispatched to userId={}", user.getId());
        });
    }

    /**
     * Validates the reset token and changes the password, then consumes the token.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }

        SecurityToken token = securityTokenService.consumeToken(rawToken, TokenType.PASSWORD_RESET);
        User user = token.getUser();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset completed for userId={}", user.getId());
    }
}
