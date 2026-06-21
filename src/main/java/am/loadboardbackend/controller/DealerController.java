package am.loadboardbackend.controller;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.dealer.DealerResponseDto;
import am.loadboardbackend.dto.dealer.RegisterDealerRequest;
import am.loadboardbackend.dto.document.DocumentUploadResponse;
import am.loadboardbackend.model.Dealer;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.DocumentStorageService;
import am.loadboardbackend.service.PublicRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dealers")
@RequiredArgsConstructor
@Slf4j
public class DealerController {

    private final PublicRegistrationService publicRegistrationService;
    private final DocumentStorageService documentStorageService;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterDealerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(publicRegistrationService.registerDealer(req));
    }

    @GetMapping("/me")
    public DealerResponseDto me(@AuthenticationPrincipal User user) {
        Dealer d = user.getDealer();
        if (d == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer profile not found");
        return new DealerResponseDto(
                d.getCompanyName(),
                d.getOwnerFirstName(),
                d.getOwnerLastName(),
                d.getBusinessPhone(),
                d.getCompanyAddress(),
                d.getCity(),
                d.getState(),
                d.getZipCode(),
                d.getYearEstablished(),
                d.getDealerLicenseNumber(),
                d.getAuctionAccessNumber()
        );
    }

    @PostMapping("/documents/w9")
    public ResponseEntity<DocumentUploadResponse> uploadW9(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        if (user.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete dealer profile before uploading documents");
        }
        DocumentUploadResponse response = documentStorageService.storeW9(
                file, user.getDealer().getId(), "DEALER");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/documents/dealer-license")
    public ResponseEntity<DocumentUploadResponse> uploadDealerLicense(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        if (user.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete dealer profile before uploading documents");
        }
        DocumentUploadResponse response = documentStorageService.storeDealerDocument(
                file, user.getDealer().getId(), "DEALER", "DEALER_LICENSE");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/documents/corporate-paperwork")
    public ResponseEntity<DocumentUploadResponse> uploadCorporatePaperwork(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        if (user.getDealer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete dealer profile before uploading documents");
        }
        DocumentUploadResponse response = documentStorageService.storeDealerDocument(
                file, user.getDealer().getId(), "DEALER", "CORPORATE_PAPERWORK");
        return ResponseEntity.ok(response);
    }
}
