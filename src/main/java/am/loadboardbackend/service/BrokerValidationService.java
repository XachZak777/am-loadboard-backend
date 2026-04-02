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
// ...existing code...
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@Service
@Slf4j
public class BrokerValidationService {

        private final FmcsaClient fmcsaClient;
        private final TemporaryValidationStore tempStore;
        private final BrokerValidationRepository brokerValidationRepo;

        public BrokerValidationService(FmcsaClient fmcsaClient, TemporaryValidationStore tempStore, BrokerValidationRepository brokerValidationRepo) {
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

                if (carrierResponse == null ||
                                carrierResponse.getContent() == null ||
                                carrierResponse.getContent().isEmpty()) {
                        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Broker not found in FMCSA");
                }

        String dotNumber = carrierResponse.getContent().get(0).getCarrier().getDotNumber();

        // Normalize DOT number to digits only (FMCSA endpoints commonly expect numeric DOT)
        String normalizedDot = dotNumber == null ? null : dotNumber.replaceAll("\\D", "");
        log.info("Resolved dotNumber='{}' normalized='{}' for lookup", dotNumber, normalizedDot);

        FmcsaAuthorityResponse authorityResponse = fmcsaClient.fetchAuthority(normalizedDot);
        log.info("Authority response for DOT {}: {}", normalizedDot, authorityResponse);

        // Do not fail the preview if authority is inactive; include the authority flag in the response
        BrokerValidationResult result = BrokerValidationResult.from(
                carrierResponse,
                authorityResponse
        );

                // Ensure we cache a canonical MC number: prefer parsed MC, else fall back to the lookup value when the lookup was MC
                String mcToCache = result.getMcNumber();
                if (mcToCache == null && lookupType == CarrierLookupType.MC) {
                        mcToCache = value;
                }

        var carrier = carrierResponse.getContent().get(0).getCarrier();

        ValidationPayload payload = new ValidationPayload(
                value,
                lookupType,
                carrier.getDotNumber(),
                carrier.getMcNumber(),
                carrier.getLegalName(),
                carrier.getDbaName(),
                carrier.getStatusCode(),
                carrier.getAllowedToOperate(),
                carrier.getPhyStreet(),
                carrier.getPhyCity(),
                carrier.getPhyState(),
                carrier.getPhyZipcode(),
                carrier.getPhyCountry(),
                carrier.getTotalDrivers(),
                carrier.getTotalPowerUnits(),
                result.isBrokerAuthorityActive()
        );

        // persist broker validation so cache miss fallback works
        BrokerValidation bv = new BrokerValidation();
        bv.setMcNumber(mcToCache);
        bv.setDotNumber(payload.dotNumber);
        bv.setLegalName(payload.legalName);
        bv.setOperatingStatus(payload.operatingStatus);
        bv.setBrokerAuthorityActive(Boolean.TRUE.equals(payload.brokerAuthorityActive));

        BrokerValidation saved = null;
                                try {
                                        saved = brokerValidationRepo.save(bv);
                                } catch (ObjectOptimisticLockingFailureException e) {
                                        log.warn("Optimistic lock failure while saving BrokerValidation; continuing.", e);
                                }

        java.util.UUID id = saved != null ? saved.getId() : java.util.UUID.randomUUID();

                // if payload.mcNumber is null but we computed a canonical mcToCache, persist it into the brokerMcCache
                if (payload.mcNumber == null && mcToCache != null) {
                        tempStore.storeBrokerMcNumber(id, mcToCache);
                }

                tempStore.storeValidation(id, payload);
        log.info("Broker validation cached id={} mcNumber={}", id, mcToCache);

        return new LookupResponse(
                id,
                lookupType.name(),
                value,
                payload.dotNumber,
                // prefer parsed mcNumber when present, else return canonical mcToCache
                payload.mcNumber != null ? payload.mcNumber : mcToCache,
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

    // Keep old validate method (non-persisting) for registration flow — add overload to match carrier service
    public BrokerValidationResult validate(String value, CarrierLookupType type) {
        FmcsaCarrierResponse response =
                (type == CarrierLookupType.DOT)
                        ? fmcsaClient.fetchByDot(value)
                        : fmcsaClient.fetchByMc(value);

                if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Broker not found in FMCSA");
                }

        String dotNumber = response.getContent().get(0).getCarrier().getDotNumber();
        String normalizedDot = dotNumber == null ? null : dotNumber.replaceAll("\\D", "");
        FmcsaAuthorityResponse authorityResponse = fmcsaClient.fetchAuthority(normalizedDot);

        // Do not throw on inactive authority; return result and let caller decide
        return BrokerValidationResult.from(response, authorityResponse);
    }

    // Backwards-compatible single-arg validate for existing callers (assumes MC)
    public BrokerValidationResult validate(String mcNumber) {
        return validate(mcNumber, CarrierLookupType.MC);
    }
}
