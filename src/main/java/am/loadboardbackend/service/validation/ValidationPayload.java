package am.loadboardbackend.service.validation;

import am.loadboardbackend.dto.CarrierLookupType;

public class ValidationPayload {

    public final String lookupValue;
    public final CarrierLookupType lookupType;

    public final String dotNumber;
    public final String mcNumber;
    public final String legalName;
    public final String dbaName;
    public final String operatingStatus;
    public final String allowedToOperate;
    public final String phyStreet;
    public final String phyCity;
    public final String phyState;
    public final String phyZip;
    public final String phyCountry;
    public final Integer totalDrivers;
    public final Integer totalPowerUnits;

    public final Boolean brokerAuthorityActive;

    public ValidationPayload(
            String lookupValue,
            CarrierLookupType lookupType,
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
            Integer totalPowerUnits,
            Boolean brokerAuthorityActive
    ) {
        this.lookupValue = lookupValue;
        this.lookupType = lookupType;
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
        this.brokerAuthorityActive = brokerAuthorityActive;
    }
}
