package am.loadboardbackend.service;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PublicRegistrationService {

    private final RegistrationService registrationService;

    /**
     * Frontend-friendly registration endpoint.
     * <p>
     * The current frontend calls /api/auth/register with { email, password, role }.
     * Internally we reuse existing broker/carrier registration flows.
     */
    public LoginResponse register(RegisterRequest request) {
        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role is required");
        }

        String role = request.getRole().trim().toUpperCase();

        return switch (role) {
            case "BROKER" -> {
                String company = "Test Broker Co " + Math.abs(request.getEmail().hashCode());
                String mc = "MC-" + Math.abs(request.getEmail().hashCode());
                String dot = "DOT-" + Math.abs(request.getEmail().hashCode());

                var req = new am.loadboardbackend.dto.auth.RegisterBrokerFromPreviewRequest(
                        request.getEmail(),
                        request.getPassword(),
                        company,
                        mc,
                        dot,
                        null,
                        null
                );
                yield registrationService.registerBrokerWithPreview(req);
            }
            case "CARRIER" -> {
                var req = new am.loadboardbackend.dto.auth.RegisterCarrierFromPreviewRequest(
                        request.getEmail(),
                        request.getPassword(),
                        "Test Carrier Co",
                        "MC-" + Math.abs(request.getEmail().hashCode()),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                yield registrationService.registerCarrierWithPreview(req);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role. Expected BROKER or CARRIER");
        };
    }
}
