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
        String phyCity,
        String phyState,
        Integer totalPowerUnits,
        String phoneNumber,
        Integer ratingScore
) {}
