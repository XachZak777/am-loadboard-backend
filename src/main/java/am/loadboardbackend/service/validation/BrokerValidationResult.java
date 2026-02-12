package am.loadboardbackend.service.validation;

import am.loadboardbackend.dto.fmcsa.FmcsaAuthorityResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrier;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import lombok.Getter;

@Getter
public class BrokerValidationResult {

    private final String mcNumber;
    private final String dotNumber;
    private final String legalName;

    private final String operatingStatus;

    private final boolean brokerAuthorityActive;
    private final String rawFmcsaJson;

    private BrokerValidationResult(
            String mcNumber,
            String dotNumber,
            String legalName,
            String operatingStatus,
            boolean brokerAuthorityActive,
            String rawFmcsaJson
    ) {
        this.mcNumber = mcNumber;
        this.dotNumber = dotNumber;
        this.legalName = legalName;
        this.operatingStatus = operatingStatus;
        this.brokerAuthorityActive = brokerAuthorityActive;
        this.rawFmcsaJson = rawFmcsaJson;
    }

    public static BrokerValidationResult from(
            FmcsaCarrierResponse carrierResponse,
            FmcsaAuthorityResponse authorityResponse,
            String rawJson
    ) {
        FmcsaCarrier carrier =
                carrierResponse.getContent().get(0).getCarrier();

        boolean brokerActive =
                authorityResponse != null &&
                        authorityResponse.isBrokerAuthorityActive();

        return new BrokerValidationResult(
                carrier.getMcNumber(),
                carrier.getDotNumber(),
                carrier.getLegalName(),
                carrier.getStatusCode(),
                brokerActive,
                rawJson
        );
    }
}
