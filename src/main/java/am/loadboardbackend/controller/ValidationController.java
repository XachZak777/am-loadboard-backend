package am.loadboardbackend.controller;

import am.loadboardbackend.dto.validation.LookupRequest;
import am.loadboardbackend.dto.validation.LookupResponse;
import am.loadboardbackend.dto.validation.SaveFromValidationRequest;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.service.BrokerValidationService;
import am.loadboardbackend.service.CarrierValidationService;
import am.loadboardbackend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/carrier")
    public LookupResponse validateCarrier(@RequestBody LookupRequest req) {
        log.info("Validation request for carrier lookupType={} lookupValue={}", req.lookupType(), req.lookupValue());
        LookupResponse resp = carrierValidationService.validateAndCache(req.lookupValue(), req.lookupType());
        log.info("Validation response id={} for carrier lookupValue={}", resp.validationId(), resp.lookupValue());
        return resp;
    }

    @PostMapping("/broker")
    public LookupResponse validateBroker(@RequestBody LookupRequest req) {
        log.info("Validation request for broker lookupType={} lookupValue={}", req.lookupType(), req.lookupValue());
        LookupResponse resp = brokerValidationService.validateAndCache(req.lookupValue(), req.lookupType());
        log.info("Validation response id={} for broker lookupValue={}", resp.validationId(), req.lookupValue());
        return resp;
    }

    @PostMapping("/carrier/save")
    public LoginResponse saveCarrierFromValidation(@RequestBody SaveFromValidationRequest req) {
        log.info("Save request for carrier validationId={} email={}", req.validationId(), req.email());
        return registrationService.registerCarrierFromValidation(req.validationId(), req.email(), req.password());
    }

    @PostMapping("/broker/save")
    public LoginResponse saveBrokerFromValidation(@RequestBody SaveFromValidationRequest req) {
        log.info("Save request for broker validationId={} email={}", req.validationId(), req.email());
        return registrationService.registerBrokerFromValidation(req.validationId(), req.email(), req.password());
    }
}
