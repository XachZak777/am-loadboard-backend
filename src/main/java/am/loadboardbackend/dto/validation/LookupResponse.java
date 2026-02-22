package am.loadboardbackend.dto.validation;

import java.util.UUID;

/**
 * Returned after validation. Contains the validationId and parsed metadata for preview.
 */
public record LookupResponse(
        UUID validationId,
        String lookupType,
        String lookupValue,
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
) {}
