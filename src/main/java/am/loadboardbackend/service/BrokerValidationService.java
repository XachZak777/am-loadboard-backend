package am.loadboardbackend.service;

import am.loadboardbackend.client.FmcsaClient;
import am.loadboardbackend.dto.carrier.CarrierLookupType;
import am.loadboardbackend.dto.fmcsa.FmcsaAuthorityResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import am.loadboardbackend.service.validation.BrokerValidationResult;
import am.loadboardbackend.service.validation.ValidationPayload;
import am.loadboardbackend.model.BrokerValidation;
import am.loadboardbackend.repository.BrokerValidationRepository;
import am.loadboardbackend.dto.validation.LookupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@Service
@Slf4j
public class BrokerValidationService {

    private final FmcsaClient fmcsaClient;
    private final TemporaryValidationStore tempStore;
    private final BrokerValidationRepository brokerValidationRepo;

    public BrokerValidationService(FmcsaClient fmcsaClient, TemporaryValidationStore tempStore,
                                   BrokerValidationRepository brokerValidationRepo) {
        this.fmcsaClient = fmcsaClient;
        this.tempStore = tempStore;
        this.brokerValidationRepo = brokerValidationRepo;
    }

    public LookupResponse validateAndCache(String value, CarrierLookupType lookupType) {
        log.info("Starting broker validation for lookupType={} value={}", lookupType, value);

        FmcsaCarrierResponse carrierResponse =
                (lookupType == CarrierLookupType.DOT)
                        ? fmcsaClient.fetchByDot(value)
                        : fmcsaClient.fetchByMc(value);

        if (carrierResponse == null || carrierResponse.getContent() == null || carrierResponse.getContent().getCarrier() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Broker not found in FMCSA");
        }

        // Fetch authority for broker active status
        String rawDot = carrierResponse.getContent().getCarrier().getDotNumber();
        String normalizedDot = rawDot == null ? null : rawDot.replaceAll("\\D", "");
        FmcsaAuthorityResponse authorityResponse = fmcsaClient.fetchAuthority(normalizedDot);

        BrokerValidationResult r = BrokerValidationResult.from(carrierResponse, authorityResponse);

        String mcToCache = r.getMcNumber() != null ? r.getMcNumber()
                         : (lookupType == CarrierLookupType.MC ? value : null);

        ValidationPayload payload = new ValidationPayload(
                value, lookupType,
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
                r.isBrokerAuthorityActive()
        );

        BrokerValidation bv = new BrokerValidation();
        bv.setDotNumber(payload.dotNumber);
        bv.setMcNumber(mcToCache);
        bv.setLegalName(payload.legalName);
        bv.setDbaName(payload.dbaName);
        bv.setEntityType(payload.entityType);
        bv.setOperatingStatus(payload.operatingStatus);
        bv.setAllowedToOperate(payload.allowedToOperate);
        bv.setOutOfServiceDate(payload.outOfServiceDate);
        bv.setLatestUpdate(payload.latestUpdate);
        bv.setPhyStreet(payload.phyStreet);
        bv.setPhyCity(payload.phyCity);
        bv.setPhyState(payload.phyState);
        bv.setPhyZip(payload.phyZip);
        bv.setPhyCountry(payload.phyCountry);
        bv.setMailingStreet(payload.mailingStreet);
        bv.setMailingCity(payload.mailingCity);
        bv.setMailingState(payload.mailingState);
        bv.setMailingZip(payload.mailingZip);
        bv.setMailingCountry(payload.mailingCountry);
        bv.setPhone(payload.phone);
        bv.setTotalDrivers(payload.totalDrivers);
        bv.setTotalPowerUnits(payload.totalPowerUnits);
        bv.setOperationClassification(payload.operationClassification);
        bv.setCarrierOperation(payload.carrierOperation);
        bv.setCargoCarried(payload.cargoCarried);
        bv.setMcs150Date(payload.mcs150Date);
        bv.setMcs150Mileage(payload.mcs150Mileage);
        bv.setMcs150Year(payload.mcs150Year);
        bv.setSafetyRating(payload.safetyRating);
        bv.setSafetyRatingDate(payload.safetyRatingDate);
        bv.setSafetyReviewDate(payload.safetyReviewDate);
        bv.setSafetyType(payload.safetyType);
        bv.setBrokerAuthorityActive(Boolean.TRUE.equals(payload.brokerAuthorityActive));

        BrokerValidation saved = null;
        try {
            saved = brokerValidationRepo.save(bv);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure while saving BrokerValidation; continuing.", e);
        }

        java.util.UUID id = saved != null ? saved.getId() : java.util.UUID.randomUUID();

        if (r.getMcNumber() == null && mcToCache != null) {
            tempStore.storeBrokerMcNumber(id, mcToCache);
        }
        tempStore.storeValidation(id, payload);
        log.info("Broker validation cached id={} mcNumber={}", id, mcToCache);

        return new LookupResponse(
                id, lookupType.name(), value,
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
                payload.brokerAuthorityActive
        );
    }

    public BrokerValidationResult validate(String value, CarrierLookupType type) {
        FmcsaCarrierResponse response =
                (type == CarrierLookupType.DOT)
                        ? fmcsaClient.fetchByDot(value)
                        : fmcsaClient.fetchByMc(value);

        if (response == null || response.getContent() == null || response.getContent().getCarrier() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Broker not found in FMCSA");
        }

        String dot = response.getContent().getCarrier().getDotNumber();
        String normalizedDot = dot == null ? null : dot.replaceAll("\\D", "");
        FmcsaAuthorityResponse authorityResponse = fmcsaClient.fetchAuthority(normalizedDot);
        return BrokerValidationResult.from(response, authorityResponse);
    }

    public BrokerValidationResult validate(String mcNumber) {
        return validate(mcNumber, CarrierLookupType.MC);
    }
}
