package am.loadboardbackend.controller;

import am.loadboardbackend.dto.admin.AdminDocumentDto;
import am.loadboardbackend.dto.carrier.CarrierProfileRequest;
import am.loadboardbackend.dto.carrier.CarrierPublicDto;
import am.loadboardbackend.dto.carrier.CarrierResponseDto;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.document.DocumentUploadResponse;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.User;
import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.service.CarrierProfileService;
import am.loadboardbackend.service.CarrierService;
import am.loadboardbackend.service.DocumentStorageService;
import am.loadboardbackend.service.PreferredLoadService;
import am.loadboardbackend.service.RatingService;
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
@RequestMapping("/api/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final RegistrationService registrationService;
    private final CarrierService carrierService;
    private final CarrierProfileService carrierProfileService;
    private final DocumentStorageService documentStorageService;
    private final RatingService ratingService;
    private final PreferredLoadService preferredLoadService;

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

    /**
     * GET /api/carriers/{carrierId}/public — returns basic carrier info
     * for authenticated brokers reviewing bids. No sensitive data exposed.
     */
    @GetMapping("/{carrierId}/public")
    public ResponseEntity<CarrierPublicDto> getPublicInfo(@PathVariable UUID carrierId) {
        Carrier c = carrierService.getEntity(carrierId);
        long score = ratingService.computeRatingScore(carrierId, "carrier");
        Integer ratingScore = score >= 0 ? (int) score : null;
        return ResponseEntity.ok(toPublicDto(c, ratingScore));
    }

    /**
     * GET /api/carriers/search?q=... — search registered carriers by name, DOT or MC.
     * Requires authentication (brokers only in practice).
     */
    @GetMapping("/search")
    public ResponseEntity<List<CarrierPublicDto>> searchCarriers(@RequestParam String q) {
        if (q == null || q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        List<CarrierPublicDto> results = carrierService.search(q.trim()).stream()
                .limit(20)
                .map(c -> {
                    long score = ratingService.computeRatingScore(c.getId(), "carrier");
                    return toPublicDto(c, score >= 0 ? (int) score : null);
                })
                .toList();
        return ResponseEntity.ok(results);
    }

    /**
     * PATCH /api/carriers/profile — update (or create) carrier profile fields.
     * Sets adminApproved=false until the admin reviews the submission.
     */
    @PatchMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody CarrierProfileRequest request) {
        carrierProfileService.updateProfile(user, request);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    /**
     * POST /api/carriers/documents/w9 — upload W9 document (PDF/DOC/DOCX, max 5MB).
     */
    @PostMapping("/documents/w9")
    public ResponseEntity<DocumentUploadResponse> uploadW9(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        if (user.getCarrier() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Complete carrier profile before uploading documents");
        }
        DocumentUploadResponse response = documentStorageService.storeW9(
                file, user.getCarrier().getId(), "CARRIER");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/carriers/documents/insurance — upload Insurance Certificate (PDF/DOC/DOCX, max 5MB).
     */
    @PostMapping("/documents/insurance")
    public ResponseEntity<DocumentUploadResponse> uploadInsurance(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        if (user.getCarrier() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Complete carrier profile before uploading documents");
        }
        DocumentUploadResponse response = documentStorageService.storeInsurance(
                file, user.getCarrier().getId(), "CARRIER");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/carriers/documents/mc-authority — upload MC Authority document (PDF/DOC/DOCX, max 5MB).
     */
    @PostMapping("/documents/mc-authority")
    public ResponseEntity<DocumentUploadResponse> uploadMcAuthority(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        if (user.getCarrier() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Complete carrier profile before uploading documents");
        }
        DocumentUploadResponse response = documentStorageService.storeMcAuthority(
                file, user.getCarrier().getId(), "CARRIER");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/carriers/documents — list all documents uploaded by this carrier.
     */
    @GetMapping("/documents")
    public ResponseEntity<List<AdminDocumentDto>> listDocuments(@AuthenticationPrincipal User user) {
        if (user.getCarrier() == null) return ResponseEntity.ok(List.of());
        List<AdminDocumentDto> docs = documentStorageService
                .listDocuments(user.getCarrier().getId(), "CARRIER")
                .stream()
                .map(d -> new AdminDocumentDto(d.getId(), d.getDocumentType(), d.getOriginalName(), d.getFileUrl(), d.getUploadedAt()))
                .toList();
        return ResponseEntity.ok(docs);
    }

    /**
     * DELETE /api/carriers/documents/{documentId} — delete one of this carrier's documents.
     */
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal User user,
            @PathVariable UUID documentId) {
        if (user.getCarrier() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "No carrier profile found");
        }
        documentStorageService.deleteDocument(documentId, user.getCarrier().getId(), "CARRIER");
        return ResponseEntity.noContent().build();
    }

    private static CarrierPublicDto toPublicDto(Carrier c, Integer ratingScore) {
        return new CarrierPublicDto(
                c.getId(),
                c.getDotNumber(),
                c.getMcNumber(),
                c.getLegalName(),
                c.getDbaName(),
                c.getCompanyName(),
                c.getOperatingStatus(),
                c.getSafetyRating(),
                c.getPhyStreet(),
                c.getPhyCity(),
                c.getPhyState(),
                c.getPhyZip(),
                c.getTotalPowerUnits(),
                c.getPhoneNumber(),
                ratingScore
        );
    }

    @GetMapping("/preferred-loads")
    public ResponseEntity<List<LoadPostingDto>> getPreferredLoads(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(preferredLoadService.list(user));
    }

    @GetMapping("/preferred-loads/ids")
    public ResponseEntity<List<UUID>> getPreferredLoadIds(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(preferredLoadService.savedLoadIds(user));
    }

    @PostMapping("/preferred-loads/{loadId}")
    public ResponseEntity<Void> addPreferredLoad(
            @AuthenticationPrincipal User user,
            @PathVariable UUID loadId) {
        preferredLoadService.add(user, loadId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/preferred-loads/{loadId}")
    public ResponseEntity<Void> removePreferredLoad(
            @AuthenticationPrincipal User user,
            @PathVariable UUID loadId) {
        preferredLoadService.remove(user, loadId);
        return ResponseEntity.noContent().build();
    }
}

