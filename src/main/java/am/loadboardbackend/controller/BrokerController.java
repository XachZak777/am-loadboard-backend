package am.loadboardbackend.controller;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.broker.BrokerProfileRequest;
import am.loadboardbackend.dto.broker.BrokerPublicDto;
import am.loadboardbackend.dto.broker.BrokerResponseDto;
import am.loadboardbackend.dto.broker.RegisterBrokerRequest;
import am.loadboardbackend.dto.document.DocumentUploadResponse;
import am.loadboardbackend.dto.load.LoadResponseDto;
import am.loadboardbackend.dto.load.CreateLoadRequest;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.service.BrokerProfileService;
import am.loadboardbackend.service.BrokerService;
import am.loadboardbackend.service.DocumentStorageService;
import am.loadboardbackend.service.LoadService;
import am.loadboardbackend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/brokers")
@RequiredArgsConstructor
public class BrokerController {

    private final RegistrationService registrationService;
    private final BrokerService brokerService;
    private final LoadService loadService;
    private final BrokerProfileService brokerProfileService;
    private final DocumentStorageService documentStorageService;
    private final UserRepository userRepository;

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

    /**
     * GET /api/brokers/{brokerId}/public — returns broker contact info
     * for authenticated carriers viewing a load detail.
     */
    @GetMapping("/{brokerId}/public")
    public ResponseEntity<BrokerPublicDto> getPublicInfo(@PathVariable UUID brokerId) {
        Broker b = brokerService.getEntity(brokerId);
        String email = userRepository.findByBrokerId(brokerId)
                .map(User::getEmail)
                .orElse(null);
        return ResponseEntity.ok(new BrokerPublicDto(
                b.getMcNumber(),
                b.getDotNumber(),
                b.getLegalName(),
                b.getCompanyName(),
                b.getOperatingStatus(),
                b.getCity(),
                b.getState(),
                b.getPhoneNumber(),
                email
        ));
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

    /**
     * PATCH /api/brokers/profile — update (or create) broker profile fields.
     * Sets adminApproved=false until the admin reviews the submission.
     */
    @PatchMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody BrokerProfileRequest request) {
        brokerProfileService.updateProfile(user, request);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    /**
     * POST /api/brokers/documents/w9 — upload W9 document (PDF/DOC/DOCX, max 5MB).
     */
    @PostMapping("/documents/w9")
    public ResponseEntity<DocumentUploadResponse> uploadW9(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        if (user.getBroker() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Complete broker profile before uploading documents");
        }
        DocumentUploadResponse response = documentStorageService.storeW9(
                file, user.getBroker().getId(), "BROKER");
        return ResponseEntity.ok(response);
    }
}

