package am.loadboardbackend.controller;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.dealer.RegisterDealerRequest;
import am.loadboardbackend.dto.document.DocumentUploadResponse;
import am.loadboardbackend.model.Dealer;
import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import am.loadboardbackend.repository.DealerRepository;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.security.JwtUtil;
import am.loadboardbackend.service.DocumentStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dealers")
@RequiredArgsConstructor
@Slf4j
public class DealerController {

    private final DealerRepository dealerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final DocumentStorageService documentStorageService;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterDealerRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address is already in use");
        }

        Dealer dealer = new Dealer();
        dealer.setCompanyName(req.companyName());
        dealer.setOwnerFirstName(req.ownerFirstName());
        dealer.setOwnerLastName(req.ownerLastName());
        dealer.setBusinessPhone(req.businessPhone());
        dealer.setCompanyAddress(req.companyAddress());
        dealer.setCity(req.city());
        dealer.setState(req.state());
        dealer.setZipCode(req.zipCode());
        dealer.setYearEstablished(req.yearEstablished());
        dealer.setDealerLicenseNumber(req.dealerLicenseNumber());
        dealer.setAuctionAccessNumber(req.auctionAccessNumber());
        dealer.setHowDidYouHear(req.howDidYouHear());
        dealerRepository.save(dealer);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_DEALER);
        user.setDealer(dealer);
        user.setEmailVerified(false);
        user.setAdminApproved(false);
        userRepository.save(user);

        log.info("Dealer registered email={} userId={} dealerId={}", user.getEmail(), user.getId(), dealer.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponse(
            jwtUtil.generateToken(user),
            user.getId().toString(),
            user.getEmail(),
            "DEALER",
            user.isAdminApproved()
        ));
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
