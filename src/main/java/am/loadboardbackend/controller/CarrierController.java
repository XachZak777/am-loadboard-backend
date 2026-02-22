package am.loadboardbackend.controller;

import am.loadboardbackend.dto.carrier.CarrierResponseDto;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.RegisterCarrierRequest;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.CarrierService;
import am.loadboardbackend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final RegistrationService registrationService;
    private final CarrierService carrierService;

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterCarrierRequest request) {
        return registrationService.registerCarrier(request);
    }

    @PostMapping("/register-from-cache")
    public LoginResponse registerFromCache(@RequestBody am.loadboardbackend.dto.validation.SaveFromValidationRequest req) {
        return registrationService.registerCarrierFromValidation(req.validationId(), req.email(), req.password());
    }

    @PostMapping("/register-with-preview")
    public LoginResponse registerWithPreview(@RequestBody am.loadboardbackend.dto.auth.RegisterCarrierFromPreviewRequest req) {
        return registrationService.registerCarrierWithPreview(req);
    }

    @GetMapping("/me")
    public CarrierResponseDto me(@AuthenticationPrincipal User user) {
        if (user.getCarrier() == null) {
            throw new RuntimeException("User is not a carrier");
        }
        return carrierService.getMyCarrier(user.getCarrier().getId());
    }
}
