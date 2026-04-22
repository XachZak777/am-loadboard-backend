package am.loadboardbackend.controller;

import am.loadboardbackend.dto.auth.ForgotPasswordRequest;
import am.loadboardbackend.dto.auth.LoginRequest;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.MeResponse;
import am.loadboardbackend.dto.auth.RegisterRequest;
import am.loadboardbackend.dto.auth.RegisterAdminRequest;
import am.loadboardbackend.dto.auth.ResendVerificationRequest;
import am.loadboardbackend.dto.auth.ResetPasswordRequest;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.AuthService;
import am.loadboardbackend.service.EmailVerificationService;
import am.loadboardbackend.service.PublicRegistrationService;
import am.loadboardbackend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;
    private final PublicRegistrationService publicRegistrationService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword());
    }

    /**
     * Frontend-friendly registration endpoint.
     * The current frontend calls /api/auth/register with { email, password, role }.
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(publicRegistrationService.register(request));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<LoginResponse> registerAdmin(@RequestBody RegisterAdminRequest request) {
        LoginResponse response = registrationService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/auth/me — returns the current user's profile/approval status.
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getMe(user));
    }

    // ── Email verification ────────────────────────────────────────────────────

    /**
     * GET /api/auth/verify-email?token=...
     * Called when the user clicks the link in the verification email.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    /**
     * POST /api/auth/resend-verification
     * Resends the verification email (e.g. if the previous one expired).
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }

    // ── Password reset ────────────────────────────────────────────────────────

    /**
     * POST /api/auth/forgot-password  { "email": "..." }
     * Always returns 200 to prevent user-enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        emailVerificationService.initPasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link has been sent"));
    }

    /**
     * POST /api/auth/reset-password  { "token": "...", "newPassword": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        emailVerificationService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}


