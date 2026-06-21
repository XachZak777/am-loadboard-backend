package am.loadboardbackend.service.validation;

import am.loadboardbackend.dto.carrier.CarrierLookupType;
import am.loadboardbackend.dto.fmcsa.FmcsaCrashes;
import am.loadboardbackend.dto.fmcsa.FmcsaInspections;

import java.util.List;

public class ValidationPayload {

    public final String lookupValue;
    public final CarrierLookupType lookupType;

    // Identity
    public final String dotNumber;
    public final String mcNumber;
    public final String legalName;
    public final String dbaName;
    public final String entityType;

    // Status
    public final String operatingStatus;
    public final String allowedToOperate;
    public final String outOfServiceDate;
    public final String latestUpdate;

    // Physical address
    public final String phyStreet;
    public final String phyCity;
    public final String phyState;
    public final String phyZip;
    public final String phyCountry;

    // Mailing address
    public final String mailingStreet;
    public final String mailingCity;
    public final String mailingState;
    public final String mailingZip;
    public final String mailingCountry;

    // Contact / Fleet
    public final String phone;
    public final Integer totalDrivers;
    public final Integer totalPowerUnits;

    // Operation
    public final List<String> operationClassification;
    public final List<String> carrierOperation;
    public final List<String> cargoCarried;

    // MCS-150
    public final String mcs150Date;
    public final Integer mcs150Mileage;
    public final Integer mcs150Year;

    // Safety rating
    public final String safetyRating;
    public final String safetyRatingDate;
    public final String safetyReviewDate;
    public final String safetyType;

    // Inspections & Crashes
    public final FmcsaInspections usInspections;
    public final FmcsaInspections canadaInspections;
    public final FmcsaCrashes usCrashes;
    public final FmcsaCrashes canadaCrashes;

    // Broker-specific
    public final Boolean brokerAuthorityActive;

    public ValidationPayload(
            String lookupValue,
            CarrierLookupType lookupType,
            String dotNumber,
            String mcNumber,
            String legalName,
            String dbaName,
            String entityType,
            String operatingStatus,
            String allowedToOperate,
            String outOfServiceDate,
            String latestUpdate,
            String phyStreet,
            String phyCity,
            String phyState,
            String phyZip,
            String phyCountry,
            String mailingStreet,
            String mailingCity,
            String mailingState,
            String mailingZip,
            String mailingCountry,
            String phone,
            Integer totalDrivers,
            Integer totalPowerUnits,
            List<String> operationClassification,
            List<String> carrierOperation,
            List<String> cargoCarried,
            String mcs150Date,
            Integer mcs150Mileage,
            Integer mcs150Year,
            String safetyRating,
            String safetyRatingDate,
            String safetyReviewDate,
            String safetyType,
            FmcsaInspections usInspections,
            FmcsaInspections canadaInspections,
            FmcsaCrashes usCrashes,
            FmcsaCrashes canadaCrashes,
            Boolean brokerAuthorityActive
    ) {
        this.lookupValue = lookupValue;
        this.lookupType = lookupType;
        this.dotNumber = dotNumber;
        this.mcNumber = mcNumber;
        this.legalName = legalName;
        this.dbaName = dbaName;
        this.entityType = entityType;
        this.operatingStatus = operatingStatus;
        this.allowedToOperate = allowedToOperate;
        this.outOfServiceDate = outOfServiceDate;
        this.latestUpdate = latestUpdate;
        this.phyStreet = phyStreet;
        this.phyCity = phyCity;
        this.phyState = phyState;
        this.phyZip = phyZip;
        this.phyCountry = phyCountry;
        this.mailingStreet = mailingStreet;
        this.mailingCity = mailingCity;
        this.mailingState = mailingState;
        this.mailingZip = mailingZip;
        this.mailingCountry = mailingCountry;
        this.phone = phone;
        this.totalDrivers = totalDrivers;
        this.totalPowerUnits = totalPowerUnits;
        this.operationClassification = operationClassification;
        this.carrierOperation = carrierOperation;
        this.cargoCarried = cargoCarried;
        this.mcs150Date = mcs150Date;
        this.mcs150Mileage = mcs150Mileage;
        this.mcs150Year = mcs150Year;
        this.safetyRating = safetyRating;
        this.safetyRatingDate = safetyRatingDate;
        this.safetyReviewDate = safetyReviewDate;
        this.safetyType = safetyType;
        this.usInspections = usInspections;
        this.canadaInspections = canadaInspections;
        this.usCrashes = usCrashes;
        this.canadaCrashes = canadaCrashes;
        this.brokerAuthorityActive = brokerAuthorityActive;
    }
}
