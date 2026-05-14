package am.loadboardbackend.service;

import am.loadboardbackend.config.AppProperties;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.RegisterRequest;
import am.loadboardbackend.mailing.AccountVerificationEmailContext;
import am.loadboardbackend.model.SecurityToken;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicRegistrationService {

    private final RegistrationService registrationService;
    private final SecurityTokenService securityTokenService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    /**
     * Frontend-friendly registration endpoint.
     * Registers the user (emailVerified = false), creates a verification token,
     * and sends the confirmation email. Returns the LoginResponse so the frontend
     * can store the JWT while the email is being verified in the background.
     */
    public LoginResponse register(RegisterRequest request) {
        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role is required");
        }

        String role = request.getRole().trim().toUpperCase();
        LoginResponse response = switch (role) {
            case "BROKER" -> registrationService.registerBrokerMinimal(request.getEmail(), request.getPassword());
            case "CARRIER" -> registrationService.registerCarrierMinimal(request.getEmail(), request.getPassword());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role. Expected BROKER or CARRIER");
        };

        // Send verification email asynchronously
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User not found after registration"));

            sendVerificationEmail(user);
        } catch (Exception e) {
            // Do not fail registration if email sending fails; log the error
            log.error("Failed to send verification email for email={}: {}", request.getEmail(), e.getMessage(), e);
        }

        return response;
    }

    /**
     * Creates a fresh verification token and dispatches the email.
     * Can also be called from a "resend verification" endpoint.
     */
    public void sendVerificationEmail(User user) {
        SecurityToken token = securityTokenService.createEmailVerificationToken(user);

        AccountVerificationEmailContext ctx = new AccountVerificationEmailContext();
        ctx.init(user);
        ctx.setFrom(appProperties.getMail().getFrom());
        ctx.setToken(token.getToken());
        ctx.buildVerificationUrl(appProperties.getFrontend().getBaseUrl());

        emailService.sendEmail(ctx);
        log.info("Verification email dispatched to userId={} email={}", user.getId(), user.getEmail());
    }
}

