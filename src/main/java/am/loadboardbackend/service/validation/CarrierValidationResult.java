package am.loadboardbackend.service.validation;

import am.loadboardbackend.dto.fmcsa.FmcsaCarrier;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import lombok.Getter;

@Getter
public class CarrierValidationResult {

    private final String dotNumber;
    private final String mcNumber;
    private final String legalName;
    private final String dbaName;

    private final String operatingStatus;

    private final String allowedToOperate;

    private final String phyStreet;
    private final String phyCity;
    private final String phyState;
    private final String phyZip;
    private final String phyCountry;

    private final Integer totalDrivers;
    private final Integer totalPowerUnits;

    private CarrierValidationResult(
            String dotNumber,
            String mcNumber,
            String legalName,
            String dbaName,
            String operatingStatus,
            String allowedToOperate,
            String phyStreet,
            String phyCity,
            String phyState,
            String phyZip,
            String phyCountry,
            Integer totalDrivers,
            Integer totalPowerUnits
    ) {
        this.dotNumber = dotNumber;
        this.mcNumber = mcNumber;
        this.legalName = legalName;
        this.dbaName = dbaName;
        this.operatingStatus = operatingStatus;
        this.allowedToOperate = allowedToOperate;
        this.phyStreet = phyStreet;
        this.phyCity = phyCity;
        this.phyState = phyState;
        this.phyZip = phyZip;
        this.phyCountry = phyCountry;
        this.totalDrivers = totalDrivers;
        this.totalPowerUnits = totalPowerUnits;
    }

    public static CarrierValidationResult from(
            FmcsaCarrierResponse response) {
        FmcsaCarrier carrier =
                response.getContent().get(0).getCarrier();

        return new CarrierValidationResult(
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
                carrier.getTotalPowerUnits()
        );
    }
}
