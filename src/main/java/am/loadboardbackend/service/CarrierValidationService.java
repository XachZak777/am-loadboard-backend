package am.loadboardbackend.service;

import am.loadboardbackend.client.FmcsaClient;
import am.loadboardbackend.dto.carrier.CarrierLookupType;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import am.loadboardbackend.service.validation.CarrierValidationResult;
import am.loadboardbackend.service.validation.ValidationPayload;
import am.loadboardbackend.model.CarrierValidation;
import am.loadboardbackend.repository.CarrierValidationRepository;
import am.loadboardbackend.dto.validation.LookupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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

        if (response == null || response.getContent() == null || response.getContent().getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found in FMCSA");
        }

        CarrierValidationResult r = CarrierValidationResult.from(response);

        // Canonical MC: prefer parsed value; fall back to lookup value when lookup was MC
        String mcToCache = r.getMcNumber() != null ? r.getMcNumber()
                         : (type == CarrierLookupType.MC ? value : null);

        ValidationPayload payload = new ValidationPayload(
                value, type,
                r.getDotNumber(), mcToCache,
                r.getLegalName(), r.getDbaName(), r.getEntityType(),
                r.getOperatingStatus(), r.getAllowedToOperate(),
                r.getOutOfServiceDate(), r.getLatestUpdate(),
                r.getPhyStreet(), r.getPhyCity(), r.getPhyState(), r.getPhyZip(), r.getPhyCountry(),
                r.getMailingStreet(), r.getMailingCity(), r.getMailingState(), r.getMailingZip(), r.getMailingCountry(),
                r.getPhone(), r.getTotalDrivers(), r.getTotalPowerUnits(),
                r.getOperationClassification(), r.getCarrierOperation(), r.getCargoCarried(),
                r.getMcs150Date(), r.getMcs150Mileage(), r.getMcs150Year(),
                r.getSafetyRating(), r.getSafetyRatingDate(), r.getSafetyReviewDate(), r.getSafetyType(),
                r.getUsInspections(), r.getCanadaInspections(),
                r.getUsCrashes(), r.getCanadaCrashes(),
                null   // brokerAuthorityActive — not applicable to carriers
        );

        // Persist to carrier_validations for cache-miss fallback
        CarrierValidation cv = new CarrierValidation();
        cv.setLookupValue(value);
        cv.setLookupType(type);
        cv.setDotNumber(payload.dotNumber);
        cv.setMcNumber(mcToCache);
        cv.setLegalName(payload.legalName);
        cv.setDbaName(payload.dbaName);
        cv.setEntityType(payload.entityType);
        cv.setOperatingStatus(payload.operatingStatus);
        cv.setAllowedToOperate(payload.allowedToOperate);
        cv.setOutOfServiceDate(payload.outOfServiceDate);
        cv.setLatestUpdate(payload.latestUpdate);
        cv.setPhyStreet(payload.phyStreet);
        cv.setPhyCity(payload.phyCity);
        cv.setPhyState(payload.phyState);
        cv.setPhyZip(payload.phyZip);
        cv.setPhyCountry(payload.phyCountry);
        cv.setMailingStreet(payload.mailingStreet);
        cv.setMailingCity(payload.mailingCity);
        cv.setMailingState(payload.mailingState);
        cv.setMailingZip(payload.mailingZip);
        cv.setMailingCountry(payload.mailingCountry);
        cv.setPhone(payload.phone);
        cv.setTotalDrivers(payload.totalDrivers);
        cv.setTotalPowerUnits(payload.totalPowerUnits);
        cv.setOperationClassification(payload.operationClassification);
        cv.setCarrierOperation(payload.carrierOperation);
        cv.setCargoCarried(payload.cargoCarried);
        cv.setMcs150Date(payload.mcs150Date);
        cv.setMcs150Mileage(payload.mcs150Mileage);
        cv.setMcs150Year(payload.mcs150Year);
        cv.setSafetyRating(payload.safetyRating);
        cv.setSafetyRatingDate(payload.safetyRatingDate);
        cv.setSafetyReviewDate(payload.safetyReviewDate);
        cv.setSafetyType(payload.safetyType);

        CarrierValidation saved = null;
        try {
            saved = carrierValidationRepo.save(cv);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure while saving CarrierValidation; continuing.", e);
        }

        java.util.UUID id = saved != null ? saved.getId() : java.util.UUID.randomUUID();

        if (r.getMcNumber() == null && mcToCache != null) {
            tempStore.storeBrokerMcNumber(id, mcToCache);
        }
        tempStore.storeValidation(id, payload);
        log.info("Carrier validation cached id={} for value={}", id, value);

        return new LookupResponse(
                id, type.name(), value,
                payload.dotNumber, mcToCache,
                payload.legalName, payload.dbaName, payload.entityType,
                payload.operatingStatus, payload.allowedToOperate,
                payload.outOfServiceDate, payload.latestUpdate,
                payload.phyStreet, payload.phyCity, payload.phyState, payload.phyZip, payload.phyCountry,
                payload.mailingStreet, payload.mailingCity, payload.mailingState, payload.mailingZip, payload.mailingCountry,
                payload.phone, payload.totalDrivers, payload.totalPowerUnits,
                payload.operationClassification, payload.carrierOperation, payload.cargoCarried,
                payload.mcs150Date, payload.mcs150Mileage, payload.mcs150Year,
                payload.safetyRating, payload.safetyRatingDate, payload.safetyReviewDate, payload.safetyType,
                payload.usInspections, payload.canadaInspections,
                payload.usCrashes, payload.canadaCrashes,
                null   // brokerAuthorityActive
        );
    }

    // Keep legacy validate method (non-persisting) for flows that only need validation result
    public CarrierValidationResult validate(String value, CarrierLookupType type) {
        FmcsaCarrierResponse response =
                (type == CarrierLookupType.DOT)
                        ? fmcsaClient.fetchByDot(value)
                        : fmcsaClient.fetchByMc(value);

        if (response == null || response.getContent() == null || response.getContent().getCarrier() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found in FMCSA");
        }

        return CarrierValidationResult.from(response);
    }
}
