package am.loadboardbackend.config;

import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import am.loadboardbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@haulius.com";
    private static final String ADMIN_PASSWORD = "Test1234!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRole(UserRole.ROLE_ADMIN);
            admin.setEmailVerified(true);
            admin.setEmailVerifiedAt(LocalDateTime.now());
            admin.setAdminApproved(true);
            admin.setAdminApprovedAt(LocalDateTime.now());
            log.info("Local admin seeded: {}", ADMIN_EMAIL);
        }
        // Always reset credentials and lock state on startup
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setEmailVerified(true);
        admin.setAdminApproved(true);
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        admin.setLoginDisabled(false);
        userRepository.save(admin);
        log.info("Local admin unlocked: {}", ADMIN_EMAIL);
    }
}
