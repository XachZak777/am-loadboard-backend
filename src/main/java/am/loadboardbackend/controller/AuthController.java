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
import am.loadboardbackend.security.LoginRateLimiter;
import am.loadboardbackend.service.AuthService;
import am.loadboardbackend.service.EmailVerificationService;
import am.loadboardbackend.service.PublicRegistrationService;
import am.loadboardbackend.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;
    private final PublicRegistrationService publicRegistrationService;
    private final EmailVerificationService emailVerificationService;
    private final LoginRateLimiter loginRateLimiter;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:None}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        if (!loginRateLimiter.isAllowed(httpRequest)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please try again later.");
        }

        LoginResponse loginResponse = authService.login(request.getEmail(), request.getPassword());

        ResponseCookie jwtCookie = ResponseCookie.from("jwt", loginResponse.getToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(jwtExpirationMs))
                .sameSite(cookieSameSite)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse httpResponse) {
        ResponseCookie clearCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(publicRegistrationService.register(request));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<LoginResponse> registerAdmin(@RequestBody RegisterAdminRequest request) {
        LoginResponse response = registrationService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getMe(user));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        emailVerificationService.initPasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        emailVerificationService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
