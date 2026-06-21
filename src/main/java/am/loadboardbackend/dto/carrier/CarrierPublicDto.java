package am.loadboardbackend.dto.carrier;

/**
 * Publicly shareable carrier info returned to authenticated brokers
 * when they review bids — no sensitive financial/tax data exposed.
 */
public record CarrierPublicDto(
        java.util.UUID id,
        String dotNumber,
        String mcNumber,
        String legalName,
        String dbaName,
        String companyName,
        String operatingStatus,
        String safetyRating,
        String phyStreet,
        String phyCity,
        String phyState,
        String phyZip,
        Integer totalPowerUnits,
        String phoneNumber,
        Integer ratingScore
) {}
