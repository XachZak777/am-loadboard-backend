package am.loadboardbackend.controller;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.broker.BrokerResponseDto;
import am.loadboardbackend.dto.broker.RegisterBrokerRequest;
import am.loadboardbackend.dto.load.LoadResponseDto;
import am.loadboardbackend.dto.load.CreateLoadRequest;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.BrokerService;
import am.loadboardbackend.service.LoadService;
import am.loadboardbackend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brokers")
@RequiredArgsConstructor
public class BrokerController {

    private final RegistrationService registrationService;
    private final BrokerService brokerService;
    private final LoadService loadService;

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterBrokerRequest request) {
        return registrationService.registerBroker(request);
    }

    @PostMapping("/register-from-cache")
    public LoginResponse registerFromCache(@RequestBody am.loadboardbackend.dto.validation.SaveFromValidationRequest req) {
        return registrationService.registerBrokerFromValidation(req.validationId(), req.email(), req.password());
    }

    @PostMapping("/register-with-preview")
    public LoginResponse registerWithPreview(@RequestBody am.loadboardbackend.dto.auth.RegisterBrokerFromPreviewRequest req) {
        return registrationService.registerBrokerWithPreview(req);
    }

    @GetMapping("/me")
    public BrokerResponseDto me(@AuthenticationPrincipal User user) {
        if (user.getBroker() == null) {
            throw new RuntimeException("User is not a broker");
        }
        return brokerService.getMyBroker(user.getBroker().getId());
    }

    @PostMapping("/loads")
    public LoadResponseDto createLoad(
            @RequestBody CreateLoadRequest request,
            @AuthenticationPrincipal User user
    ) {
        return loadService.create(request, user.getBroker());
    }

    @GetMapping("/loads")
    public List<LoadResponseDto> myLoads(@AuthenticationPrincipal User user) {
        return loadService.getByBroker(user.getBroker());
    }
}
