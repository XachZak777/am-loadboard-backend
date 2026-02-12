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

    @GetMapping("/me")
    public CarrierResponseDto me(@AuthenticationPrincipal User user) {
        if (user.getCarrier() == null) {
            throw new RuntimeException("User is not a carrier");
        }
        return carrierService.getMyCarrier(user.getCarrier().getId());
    }
}
