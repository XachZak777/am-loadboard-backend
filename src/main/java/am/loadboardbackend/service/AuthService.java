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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CarrierProfileService carrierProfileService;
    private final BrokerProfileService brokerProfileService;

    public LoginResponse login(String email, String password) {
        log.info("Login attempt for userId lookup");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.isLoginDisabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been disabled. Please contact support.");
        }

        // Check if account is temporarily locked
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Account temporarily locked due to too many failed attempts. Please try again later.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            recordFailedAttempt(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email address before logging in");
        }

        if (user.getRole() != UserRole.ROLE_ADMIN && !user.isAdminApproved()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is pending admin approval. You will be notified once approved.");
        }

        // Successful login: reset lockout state
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user);
        log.info("Login success userId={}", user.getId());
        return new LoginResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
                user.isAdminApproved()
        );
    }

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

    private void recordFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            log.warn("Account locked due to failed attempts userId={}", user.getId());
        }
        userRepository.save(user);
    }

    private boolean computeProfileComplete(User user) {
        if (user.getRole() == UserRole.ROLE_CARRIER) {
            return carrierProfileService.isProfileComplete(user);
        }
        if (user.getRole() == UserRole.ROLE_BROKER) {
            return brokerProfileService.isProfileComplete(user);
        }
        return true;
    }
}
