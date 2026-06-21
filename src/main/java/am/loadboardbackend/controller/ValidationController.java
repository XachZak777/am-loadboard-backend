package am.loadboardbackend.controller;

import am.loadboardbackend.dto.validation.LookupRequest;
import am.loadboardbackend.dto.validation.LookupResponse;
import am.loadboardbackend.dto.validation.SaveFromValidationRequest;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.service.BrokerValidationService;
import am.loadboardbackend.service.CarrierValidationService;
import am.loadboardbackend.service.PublicRegistrationService;
import am.loadboardbackend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/validate")
@RequiredArgsConstructor
public class ValidationController {

    private final CarrierValidationService carrierValidationService;
    private final BrokerValidationService brokerValidationService;
    private final RegistrationService registrationService;
    private final PublicRegistrationService publicRegistrationService;
    private final UserRepository userRepository;

    @PostMapping(value = "/carrier", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LookupResponse> validateCarrier(@RequestBody LookupRequest req) {
        log.info("Validation request for carrier lookupType={} lookupValue={}", req.lookupType(), req.lookupValue());
        LookupResponse resp = carrierValidationService.validateAndCache(req.lookupValue(), req.lookupType());
        log.info("Validation response id={} for carrier lookupValue={}", resp.validationId(), resp.lookupValue());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
    }

    @PostMapping(value = "/broker", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LookupResponse> validateBroker(@RequestBody LookupRequest req) {
        log.info("Validation request for broker lookupType={} lookupValue={}", req.lookupType(), req.lookupValue());
        LookupResponse resp = brokerValidationService.validateAndCache(req.lookupValue(), req.lookupType());
        log.info("Validation response id={} for broker lookupValue={}", resp.validationId(), req.lookupValue());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
    }

    @PostMapping(value = "/carrier/save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> saveCarrierFromValidation(@RequestBody SaveFromValidationRequest req) {
        log.info("Save request for carrier validationId={} email={}", req.validationId(), req.email());
        LoginResponse resp = registrationService.registerCarrierFromValidation(req.validationId(), req.email(), req.password());
        sendVerificationEmailQuietly(req.email());
        return ResponseEntity.ok().body(resp);
    }

    @PostMapping(value = "/broker/save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> saveBrokerFromValidation(@RequestBody SaveFromValidationRequest req) {
        log.info("Save request for broker validationId={} email={}", req.validationId(), req.email());
        LoginResponse resp = registrationService.registerBrokerFromValidation(req.validationId(), req.email(), req.password());
        sendVerificationEmailQuietly(req.email());
        return ResponseEntity.ok().body(resp);
    }

    private void sendVerificationEmailQuietly(String email) {
        try {
            userRepository.findByEmail(email).ifPresent(publicRegistrationService::sendVerificationEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email for email={}: {}", email, e.getMessage(), e);
        }
    }
}
