package am.loadboardbackend.dto.fmcsa;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmcsaCarrier {

    // ── Identity ──────────────────────────────────────────────────────────────

    /** REST API returns a Number; Jackson coerces it to String. */
    @JsonAlias({"usdot", "dotNumber"})
    private String dotNumber;

    @JsonAlias({"mc_mx_ff_numbers", "mcNumber"})
    private String mcNumber;

    @JsonAlias({"legal_name", "legalName"})
    private String legalName;

    @JsonAlias({"dba_name", "dbaName"})
    private String dbaName;

    @JsonAlias({"entity_type", "entityType"})
    private String entityType;

    // ── Status ────────────────────────────────────────────────────────────────

    /** FMCSA REST: "A" = Active. */
    private String statusCode;

    @JsonAlias({"operating_status", "operatingStatus"})
    private String operatingStatus;

    private String allowedToOperate;

    @JsonAlias({"oosDate", "out_of_service_date", "outOfServiceDate"})
    private String outOfServiceDate;

    @JsonAlias({"latest_update", "latestUpdate"})
    private String latestUpdate;

    // ── Physical address ──────────────────────────────────────────────────────

    @JsonAlias({"physical_address"})
    private String physicalAddressRaw;

    private String phyStreet;
    private String phyCity;
    private String phyState;

    @JsonAlias({"phyZipcode", "phyZip"})
    private String phyZipcode;

    private String phyCountry;

    // ── Mailing address ───────────────────────────────────────────────────────

    @JsonAlias({"mailing_address"})
    private String mailingAddressRaw;

    private String mailingStreet;
    private String mailingCity;
    private String mailingState;
    private String mailingZip;
    private String mailingCountry;

    // ── Contact ───────────────────────────────────────────────────────────────

    @JsonAlias({"phone", "phyPhone", "telephone"})
    private String phone;

    // ── Fleet ─────────────────────────────────────────────────────────────────

    @JsonAlias({"power_units", "nbr_of_power_unit"})
    private Integer totalPowerUnits;

    @JsonAlias({"drivers"})
    private Integer totalDrivers;

    // ── Operation ─────────────────────────────────────────────────────────────

    /**
     * REST API: nested object {"carrierOperationCode":"A","carrierOperationDesc":"Interstate"}.
     */
    private FmcsaCarrierOperation carrierOperation;

    @JsonAlias({"operation_classification", "operationClassification"})
    private List<String> operationClassification;

    @JsonAlias({"cargo_carried", "cargoCarried"})
    private List<String> cargoCarried;

    // ── MCS-150 ───────────────────────────────────────────────────────────────

    @JsonAlias({"mcs_150_form_date", "mcs150Date"})
    private String mcs150Date;

    private Integer mcs150Mileage;
    private Integer mcs150Year;

    // ── Safety rating ─────────────────────────────────────────────────────────

    @JsonAlias({"safety_rating"})
    private String safetyRating;

    @JsonAlias({"safetyRatingDate", "safety_rating_date"})
    private String safetyRatingDate;

    @JsonAlias({"safetyReviewDate", "safety_review_date", "reviewDate"})
    private String safetyReviewDate;

    @JsonAlias({"safetyReviewType", "safety_type", "reviewType"})
    private String safetyType;

    // ── Inspections — flat fields from FMCSA REST API ─────────────────────────

    private Integer vehicleInsp;
    private Integer vehicleOosInsp;
    private Double  vehicleOosRate;
    private String  vehicleOosRateNationalAverage;

    private Integer driverInsp;
    private Integer driverOosInsp;
    private Double  driverOosRate;
    private String  driverOosRateNationalAverage;

    private Integer hazmatInsp;
    private Integer hazmatOosInsp;
    private Double  hazmatOosRate;
    private String  hazmatOosRateNationalAverage;

    // ── Crashes — flat fields from FMCSA REST API ─────────────────────────────

    private Integer crashTotal;
    private Integer fatalCrash;
    private Integer injCrash;
    private Integer towawayCrash;
}
