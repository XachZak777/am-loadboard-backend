package am.loadboardbackend.controller;

import am.loadboardbackend.dto.CarrierLookupType;
import am.loadboardbackend.dto.CarrierResponseDto;
import am.loadboardbackend.dto.LoginResponse;
import am.loadboardbackend.dto.RegisterCarrierRequest;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.service.CarrierValidationService;
import am.loadboardbackend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final RegistrationService registrationService;
    private final CarrierValidationService validationService;
    private final CarrierRepository carrierRepo;

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterCarrierRequest req) {
        return registrationService.registerCarrier(req);
    }

    @GetMapping("/lookup")
    public CarrierResponseDto lookup(@RequestParam String value, @RequestParam CarrierLookupType type) {
        return validationService.validate(value, type);
    }

    @GetMapping
    public List<Carrier> getAll() {
        return carrierRepo.findAll();
    }
}
