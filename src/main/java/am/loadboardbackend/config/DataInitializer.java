package am.loadboardbackend.config;

import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import am.loadboardbackend.repository.BrokerRepository;
import am.loadboardbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * DataInitializer automatically creates default admin and broker users on application startup.
 * This helps with local development and testing.
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BrokerRepository brokerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting database initialization...");
        
        initializeAdmin();
        initializeBrokers();
        
        log.info("Database initialization completed successfully!");
    }

    /**
     * Create the default admin user if it doesn't exist.
     */
    private void initializeAdmin() {
        String adminEmail = "admin@haulius.com";
        
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists: {}", adminEmail);
            return;
        }
        
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode("Test1234"));
        admin.setRole(UserRole.ROLE_ADMIN);
        admin.setEmailVerified(true);
        admin.setEmailVerifiedAt(LocalDateTime.now());
        admin.setAdminApproved(true);
        admin.setAdminApprovedAt(LocalDateTime.now());
        
        userRepository.save(admin);
        log.info("Created admin user: {}", adminEmail);
    }

    /**
     * Create sample broker users if they don't exist.
     */
    private void initializeBrokers() {
        String[] brokerEmails = {
            "broker1@haulius.com",
            "broker2@haulius.com",
            "broker3@haulius.com"
        };
        
        for (int i = 0; i < brokerEmails.length; i++) {
            String email = brokerEmails[i];
            
            if (userRepository.findByEmail(email).isPresent()) {
                log.info("Broker user already exists: {}", email);
                continue;
            }
            
            // Create Broker entity
            Broker broker = new Broker();
            broker.setMcNumber("MC-" + String.format("%06d", 100000 + i));
            broker.setDotNumber("DOT-" + String.format("%06d", 200000 + i));
            broker.setLegalName("Broker Company " + (i + 1));
            broker.setCompanyName("Broker Company " + (i + 1));
            broker.setOperatingStatus("A");
            broker.setBrokerAuthorityActive(true);
            broker.setPhoneNumber("555-000-" + String.format("%04d", 1000 + i));
            broker.setMailingAddress("123 Business St");
            broker.setCity("Chicago");
            broker.setState("IL");
            broker.setZipCode("60601");
            broker.setCreatedAt(LocalDateTime.now());
            
            brokerRepository.save(broker);
            
            // Create User entity linked to Broker
            User brokerUser = new User();
            brokerUser.setEmail(email);
            brokerUser.setPasswordHash(passwordEncoder.encode("Test1234"));
            brokerUser.setRole(UserRole.ROLE_BROKER);
            brokerUser.setBroker(broker);
            brokerUser.setEmailVerified(true);
            brokerUser.setEmailVerifiedAt(LocalDateTime.now());
            brokerUser.setAdminApproved(true);
            brokerUser.setAdminApprovedAt(LocalDateTime.now());
            
            userRepository.save(brokerUser);
            log.info("Created broker user: {} with MC: {}", email, broker.getMcNumber());
        }
    }
}
