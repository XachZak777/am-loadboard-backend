package am.loadboardbackend.service;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.MeResponse;
import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CarrierProfileService carrierProfileService;
    private final BrokerProfileService brokerProfileService;

    public LoginResponse login(String email, String password) {
        log.info("Login attempt email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found email={}", email);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: invalid password email={}", email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (user.isLoginDisabled()) {
            log.warn("Login blocked: account disabled email={}", email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been disabled. Please contact support.");
        }

        if (!user.isEmailVerified()) {
            log.warn("Login blocked: email not verified email={}", email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email address before logging in");
        }

        // Admin users bypass the admin-approval check (they are their own approvers)
        if (user.getRole() != UserRole.ROLE_ADMIN && !user.isAdminApproved()) {
            log.warn("Login blocked: admin approval pending email={}", email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is pending admin approval. You will be notified once approved.");
        }

        String token = jwtUtil.generateToken(user);
        log.info("Login success email={} userId={}", email, user.getId());
        return new LoginResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
                user.isAdminApproved()
        );
    }

    /**
     * Returns the current authenticated user's status for GET /api/auth/me.
     */
    public MeResponse getMe(User user) {
        boolean profileComplete = computeProfileComplete(user);

        return new MeResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
                user.isAdminApproved(),
                user.isEmailVerified(),
                profileComplete
        );
    }

    public am.loadboardbackend.model.User currentUserOrThrow() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof am.loadboardbackend.model.User user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private boolean computeProfileComplete(User user) {
        if (user.getRole() == UserRole.ROLE_CARRIER) {
            return carrierProfileService.isProfileComplete(user);
        }
        if (user.getRole() == UserRole.ROLE_BROKER) {
            return brokerProfileService.isProfileComplete(user);
        }
        // Admins are always "complete"
        return true;
    }
}

