package am.loadboardbackend.service;

import am.loadboardbackend.client.FmcsaClient;
import am.loadboardbackend.dto.CarrierLookupType;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import am.loadboardbackend.service.validation.CarrierValidationResult;
import am.loadboardbackend.service.validation.ValidationPayload;
import am.loadboardbackend.service.TemporaryValidationStore;
import am.loadboardbackend.model.CarrierValidation;
import am.loadboardbackend.repository.CarrierValidationRepository;
import am.loadboardbackend.dto.validation.LookupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarrierValidationService {

    private final FmcsaClient fmcsaClient;
    private final TemporaryValidationStore tempStore;
    private final CarrierValidationRepository carrierValidationRepo;

    public LookupResponse validateAndCache(String value, CarrierLookupType type) {
        log.info("Starting carrier validation for type={} value={}", type, value);

        FmcsaCarrierResponse response =
                (type == CarrierLookupType.DOT)
                        ? fmcsaClient.fetchByDot(value)
                        : fmcsaClient.fetchByMc(value);

        if (response == null ||
                response.getContent() == null ||
                response.getContent().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found in FMCSA");
        }

        CarrierValidationResult result = CarrierValidationResult.from(response);

        ValidationPayload payload = new ValidationPayload(
                value,
                type,
                result.getDotNumber(),
                result.getMcNumber(),
                result.getLegalName(),
                result.getDbaName(),
                result.getOperatingStatus(),
                result.getAllowedToOperate(),
                result.getPhyStreet(),
                result.getPhyCity(),
                result.getPhyState(),
                result.getPhyZip(),
                result.getPhyCountry(),
                result.getTotalDrivers(),
                result.getTotalPowerUnits(),
                null
        );

    java.util.UUID id = java.util.UUID.randomUUID();

    // persist to carrier_validations so cache misses can fall back to DB
    CarrierValidation cv = new CarrierValidation();
    cv.setId(id);
    cv.setLookupValue(value);
    cv.setLookupType(type);
    cv.setDotNumber(payload.dotNumber);
    cv.setMcNumber(payload.mcNumber);
    cv.setLegalName(payload.legalName);
    cv.setDbaName(payload.dbaName);
    cv.setOperatingStatus(payload.operatingStatus);
    cv.setAllowedToOperate(payload.allowedToOperate);
    cv.setPhyStreet(payload.phyStreet);
    cv.setPhyCity(payload.phyCity);
    cv.setPhyState(payload.phyState);
    cv.setPhyZip(payload.phyZip);
    cv.setPhyCountry(payload.phyCountry);
    cv.setTotalDrivers(payload.totalDrivers);
    cv.setTotalPowerUnits(payload.totalPowerUnits);

    carrierValidationRepo.save(cv);

    tempStore.storeValidation(id, payload);
        log.info("Carrier validation cached id={} for value={}", id, value);

        return new LookupResponse(
                id,
                type.name(),
                value,
                payload.dotNumber,
                payload.mcNumber,
                payload.legalName,
                payload.dbaName,
                payload.operatingStatus,
                payload.allowedToOperate,
                payload.phyStreet,
                payload.phyCity,
                payload.phyState,
                payload.phyZip,
                payload.phyCountry,
                payload.totalDrivers,
                payload.totalPowerUnits,
                payload.brokerAuthorityActive
        );
    }

    // Keep old validate method (non-persisting) for registration flow
    public CarrierValidationResult validate(String value, CarrierLookupType type) {
        FmcsaCarrierResponse response =
                (type == CarrierLookupType.DOT)
                        ? fmcsaClient.fetchByDot(value)
                        : fmcsaClient.fetchByMc(value);

        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found in FMCSA");
        }

        return CarrierValidationResult.from(response);
    }
}
