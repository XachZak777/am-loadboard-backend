package am.loadboardbackend.service.validation;

import am.loadboardbackend.dto.fmcsa.FmcsaAuthorityResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaCrashes;
import am.loadboardbackend.dto.fmcsa.FmcsaInspections;
import lombok.Getter;

import java.util.List;

@Getter
public class BrokerValidationResult {

    // Identity
    private final String dotNumber;
    private final String mcNumber;
    private final String legalName;
    private final String dbaName;
    private final String entityType;

    // Status
    private final String operatingStatus;
    private final String allowedToOperate;
    private final String outOfServiceDate;
    private final String latestUpdate;

    // Physical address
    private final String phyStreet;
    private final String phyCity;
    private final String phyState;
    private final String phyZip;
    private final String phyCountry;

    // Mailing address
    private final String mailingStreet;
    private final String mailingCity;
    private final String mailingState;
    private final String mailingZip;
    private final String mailingCountry;

    // Contact / Fleet
    private final String phone;
    private final Integer totalDrivers;
    private final Integer totalPowerUnits;

    // Operation
    private final List<String> operationClassification;
    private final List<String> carrierOperation;
    private final List<String> cargoCarried;

    // MCS-150
    private final String mcs150Date;
    private final Integer mcs150Mileage;
    private final Integer mcs150Year;

    // Safety
    private final String safetyRating;
    private final String safetyRatingDate;
    private final String safetyReviewDate;
    private final String safetyType;

    // Inspections & Crashes
    private final FmcsaInspections usInspections;
    private final FmcsaInspections canadaInspections;
    private final FmcsaCrashes usCrashes;
    private final FmcsaCrashes canadaCrashes;

    // Broker-specific
    private final boolean brokerAuthorityActive;

    public static BrokerValidationResult from(
            FmcsaCarrierResponse response,
            FmcsaAuthorityResponse authorityResponse
    ) {
        // Reuse the carrier result logic then add broker authority flag
        CarrierValidationResult base = CarrierValidationResult.from(response);
        boolean brokerActive = authorityResponse != null && authorityResponse.isBrokerAuthorityActive();
        return new BrokerValidationResult(base, brokerActive);
    }

    private BrokerValidationResult(CarrierValidationResult base, boolean brokerAuthorityActive) {
        this.dotNumber              = base.getDotNumber();
        this.mcNumber               = base.getMcNumber();
        this.legalName              = base.getLegalName();
        this.dbaName                = base.getDbaName();
        this.entityType             = base.getEntityType();
        this.operatingStatus        = base.getOperatingStatus();
        this.allowedToOperate       = base.getAllowedToOperate();
        this.outOfServiceDate       = base.getOutOfServiceDate();
        this.latestUpdate           = base.getLatestUpdate();
        this.phyStreet              = base.getPhyStreet();
        this.phyCity                = base.getPhyCity();
        this.phyState               = base.getPhyState();
        this.phyZip                 = base.getPhyZip();
        this.phyCountry             = base.getPhyCountry();
        this.mailingStreet          = base.getMailingStreet();
        this.mailingCity            = base.getMailingCity();
        this.mailingState           = base.getMailingState();
        this.mailingZip             = base.getMailingZip();
        this.mailingCountry         = base.getMailingCountry();
        this.phone                  = base.getPhone();
        this.totalDrivers           = base.getTotalDrivers();
        this.totalPowerUnits        = base.getTotalPowerUnits();
        this.operationClassification = base.getOperationClassification();
        this.carrierOperation       = base.getCarrierOperation();
        this.cargoCarried           = base.getCargoCarried();
        this.mcs150Date             = base.getMcs150Date();
        this.mcs150Mileage          = base.getMcs150Mileage();
        this.mcs150Year             = base.getMcs150Year();
        this.safetyRating           = base.getSafetyRating();
        this.safetyRatingDate       = base.getSafetyRatingDate();
        this.safetyReviewDate       = base.getSafetyReviewDate();
        this.safetyType             = base.getSafetyType();
        this.usInspections          = base.getUsInspections();
        this.canadaInspections      = base.getCanadaInspections();
        this.usCrashes              = base.getUsCrashes();
        this.canadaCrashes          = base.getCanadaCrashes();
        this.brokerAuthorityActive  = brokerAuthorityActive;
    }
}
