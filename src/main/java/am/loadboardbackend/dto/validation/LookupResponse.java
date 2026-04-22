package am.loadboardbackend.dto.validation;

import java.util.List;
import java.util.UUID;

/**
 * Returned after a successful FMCSA validation.
 * Contains the validationId (needed for account creation) and all carrier/broker
 * metadata parsed from the SAFER / FMCSA REST response for the preview step.
 */
public record LookupResponse(
        // ── Session ──────────────────────────────────────────────────────────
        UUID validationId,
        String lookupType,
        String lookupValue,

        // ── Identity ─────────────────────────────────────────────────────────
        String dotNumber,
        String mcNumber,
        String legalName,
        String dbaName,
        String entityType,

        // ── Status ───────────────────────────────────────────────────────────
        String operatingStatus,
        String allowedToOperate,
        String outOfServiceDate,
        String latestUpdate,

        // ── Physical address ─────────────────────────────────────────────────
        String phyStreet,
        String phyCity,
        String phyState,
        String phyZip,
        String phyCountry,

        // ── Mailing address ──────────────────────────────────────────────────
        String mailingStreet,
        String mailingCity,
        String mailingState,
        String mailingZip,
        String mailingCountry,

        // ── Contact / Fleet ──────────────────────────────────────────────────
        String phone,
        Integer totalDrivers,
        Integer totalPowerUnits,

        // ── Operation ────────────────────────────────────────────────────────
        List<String> operationClassification,
        List<String> carrierOperation,
        List<String> cargoCarried,

        // ── MCS-150 ──────────────────────────────────────────────────────────
        String mcs150Date,
        Integer mcs150Mileage,
        Integer mcs150Year,

        // ── Safety rating ─────────────────────────────────────────────────────
        String safetyRating,
        String safetyRatingDate,
        String safetyReviewDate,
        String safetyType,

        // ── Inspections ──────────────────────────────────────────────────────
        am.loadboardbackend.dto.fmcsa.FmcsaInspections usInspections,
        am.loadboardbackend.dto.fmcsa.FmcsaInspections canadaInspections,

        // ── Crashes ──────────────────────────────────────────────────────────
        am.loadboardbackend.dto.fmcsa.FmcsaCrashes usCrashes,
        am.loadboardbackend.dto.fmcsa.FmcsaCrashes canadaCrashes,

        // ── Broker-specific ───────────────────────────────────────────────────
        Boolean brokerAuthorityActive
) {}
