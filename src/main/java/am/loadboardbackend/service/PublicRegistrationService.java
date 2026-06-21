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
     * Single-step carrier registration — creates user + full carrier profile in one
     * transaction, then sends the verification email. No separate profile-update call
     * is required, so the admin sees all data immediately after sign-up.
     */
    public LoginResponse registerCarrierFull(am.loadboardbackend.dto.auth.RegisterCarrierFullRequest req) {
        LoginResponse response = registrationService.registerCarrierFull(req);
        try {
            User user = userRepository.findByEmail(req.email())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "User not found after registration"));
            sendVerificationEmail(user);
        } catch (Exception e) {
            log.error("Failed to send verification email for email={}: {}", req.email(), e.getMessage(), e);
        }
        return response;
    }

    public LoginResponse registerBrokerFull(am.loadboardbackend.dto.auth.RegisterBrokerFullRequest req) {
        LoginResponse response = registrationService.registerBrokerFull(req);
        try {
            User user = userRepository.findByEmail(req.email())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "User not found after registration"));
            sendVerificationEmail(user);
        } catch (Exception e) {
            log.error("Failed to send verification email for email={}: {}", req.email(), e.getMessage(), e);
        }
        return response;
    }

    public LoginResponse registerDealer(am.loadboardbackend.dto.dealer.RegisterDealerRequest req) {
        LoginResponse response = registrationService.registerDealer(req);
        try {
            User user = userRepository.findByEmail(req.email())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "User not found after registration"));
            sendVerificationEmail(user);
        } catch (Exception e) {
            log.error("Failed to send verification email for email={}: {}", req.email(), e.getMessage(), e);
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

