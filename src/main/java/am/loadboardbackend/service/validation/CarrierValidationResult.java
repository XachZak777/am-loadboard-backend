package am.loadboardbackend.service.validation;

import am.loadboardbackend.dto.fmcsa.FmcsaCarrier;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaCrashes;
import am.loadboardbackend.dto.fmcsa.FmcsaInspectionStats;
import am.loadboardbackend.dto.fmcsa.FmcsaInspections;
import lombok.Getter;

import java.util.List;

@Getter
public class CarrierValidationResult {

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

    private CarrierValidationResult(FmcsaCarrier c) {
        this.dotNumber              = c.getDotNumber();
        // Strip "MC-" prefix that SAFER prepends (e.g. "MC-146894" → "146894")
        String mc = c.getMcNumber();
        this.mcNumber               = (mc != null && mc.toUpperCase().startsWith("MC-")) ? mc.substring(3) : mc;
        this.legalName              = c.getLegalName();
        this.dbaName                = c.getDbaName();
        this.entityType             = c.getEntityType();
        // Prefer explicit statusCode (FMCSA REST), fall back to operatingStatus (SAFER)
        this.operatingStatus        = c.getStatusCode() != null ? c.getStatusCode() : c.getOperatingStatus();
        this.allowedToOperate       = c.getAllowedToOperate();
        this.outOfServiceDate       = c.getOutOfServiceDate();
        this.latestUpdate           = c.getLatestUpdate();

        // Physical — when individual fields are absent (SAFER sends a single address string), parse them
        if (c.getPhyStreet() != null) {
            this.phyStreet  = c.getPhyStreet();
            this.phyCity    = c.getPhyCity();
            this.phyState   = c.getPhyState();
            this.phyZip     = c.getPhyZipcode();
            this.phyCountry = c.getPhyCountry();
        } else {
            // SAFER "1000 SOUTH LAWRENCE ST MOBILE, AL  36603" — best-effort parse
            String[] parts = parseAddress(c.getPhysicalAddressRaw());
            this.phyStreet  = parts[0];
            this.phyCity    = parts[1];
            this.phyState   = parts[2];
            this.phyZip     = parts[3];
            this.phyCountry = null;
        }

        // Mailing
        if (c.getMailingStreet() != null) {
            this.mailingStreet  = c.getMailingStreet();
            this.mailingCity    = c.getMailingCity();
            this.mailingState   = c.getMailingState();
            this.mailingZip     = c.getMailingZip();
            this.mailingCountry = c.getMailingCountry();
        } else {
            String[] parts = parseAddress(c.getMailingAddressRaw());
            this.mailingStreet  = parts[0];
            this.mailingCity    = parts[1];
            this.mailingState   = parts[2];
            this.mailingZip     = parts[3];
            this.mailingCountry = null;
        }

        this.phone                  = c.getPhone();
        this.totalDrivers           = c.getTotalDrivers();
        this.totalPowerUnits        = c.getTotalPowerUnits();
        this.operationClassification = c.getOperationClassification();
        // REST API returns carrierOperation as a nested object; unwrap to list
        if (c.getCarrierOperation() != null && c.getCarrierOperation().getCarrierOperationDesc() != null) {
            this.carrierOperation = java.util.List.of(c.getCarrierOperation().getCarrierOperationDesc());
        } else {
            this.carrierOperation = null;
        }
        this.cargoCarried           = c.getCargoCarried();
        this.mcs150Date             = c.getMcs150Date();
        this.mcs150Mileage          = c.getMcs150Mileage();
        this.mcs150Year             = c.getMcs150Year();
        this.safetyRating           = c.getSafetyRating();
        this.safetyRatingDate       = c.getSafetyRatingDate();
        this.safetyReviewDate       = c.getSafetyReviewDate();
        this.safetyType             = c.getSafetyType();

        // Build FmcsaInspections from flat REST API fields
        FmcsaInspections insp = new FmcsaInspections();
        FmcsaInspectionStats vehicle = new FmcsaInspectionStats();
        vehicle.setInspections(c.getVehicleInsp() != null ? String.valueOf(c.getVehicleInsp()) : null);
        vehicle.setOutOfService(c.getVehicleOosInsp() != null ? String.valueOf(c.getVehicleOosInsp()) : null);
        vehicle.setOutOfServicePercent(c.getVehicleOosRate() != null ? c.getVehicleOosRate() + "%" : null);
        vehicle.setNationalAverage(c.getVehicleOosRateNationalAverage());
        insp.setVehicle(vehicle);

        FmcsaInspectionStats driver = new FmcsaInspectionStats();
        driver.setInspections(c.getDriverInsp() != null ? String.valueOf(c.getDriverInsp()) : null);
        driver.setOutOfService(c.getDriverOosInsp() != null ? String.valueOf(c.getDriverOosInsp()) : null);
        driver.setOutOfServicePercent(c.getDriverOosRate() != null ? c.getDriverOosRate() + "%" : null);
        driver.setNationalAverage(c.getDriverOosRateNationalAverage());
        insp.setDriver(driver);

        FmcsaInspectionStats hazmat = new FmcsaInspectionStats();
        hazmat.setInspections(c.getHazmatInsp() != null ? String.valueOf(c.getHazmatInsp()) : null);
        hazmat.setOutOfService(c.getHazmatOosInsp() != null ? String.valueOf(c.getHazmatOosInsp()) : null);
        hazmat.setOutOfServicePercent(c.getHazmatOosRate() != null ? c.getHazmatOosRate() + "%" : null);
        hazmat.setNationalAverage(c.getHazmatOosRateNationalAverage());
        insp.setHazmat(hazmat);

        this.usInspections     = insp;
        this.canadaInspections = null; // not returned by REST API

        // Build FmcsaCrashes from flat REST API fields
        FmcsaCrashes crashes = new FmcsaCrashes();
        crashes.setTotal(c.getCrashTotal());
        crashes.setFatal(c.getFatalCrash());
        crashes.setInjury(c.getInjCrash());
        crashes.setTow(c.getTowawayCrash());
        this.usCrashes     = crashes;
        this.canadaCrashes = null;
    }

    public static CarrierValidationResult from(FmcsaCarrierResponse response) {
        FmcsaCarrier carrier = response.getContent().getCarrier();
        return new CarrierValidationResult(carrier);
    }

    /**
     * Best-effort parse of a SAFER combined address string like:
     * "1000 SOUTH LAWRENCE ST MOBILE, AL  36603"
     * Returns [street, city, state, zip].
     */
    private static String[] parseAddress(String raw) {
        if (raw == null || raw.isBlank()) return new String[]{null, null, null, null};
        // Split on last comma to separate "street city" from "ST  ZIP"
        int commaIdx = raw.lastIndexOf(',');
        if (commaIdx < 0) return new String[]{raw.trim(), null, null, null};
        String stateZip = raw.substring(commaIdx + 1).trim();      // "AL  36603"
        String streetCity = raw.substring(0, commaIdx).trim();      // "1000 SOUTH LAWRENCE ST MOBILE"
        String[] stateZipParts = stateZip.split("\\s+", 2);
        String state = stateZipParts.length > 0 ? stateZipParts[0] : null;
        String zip   = stateZipParts.length > 1 ? stateZipParts[1] : null;
        // Split street from city at last space-separated word boundary before known separators
        // Heuristic: last token of streetCity is city if >= 4 chars; otherwise full string is street
        int lastSpace = streetCity.lastIndexOf(' ');
        String street = lastSpace > 0 ? streetCity.substring(0, lastSpace).trim() : streetCity;
        String city   = lastSpace > 0 ? streetCity.substring(lastSpace + 1).trim() : null;
        return new String[]{street, city, state, zip};
    }
}
