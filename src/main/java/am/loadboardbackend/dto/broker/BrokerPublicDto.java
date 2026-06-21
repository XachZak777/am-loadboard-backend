package am.loadboardbackend.dto.broker;

import java.util.UUID;

/**
 * Publicly shareable broker contact info returned to authenticated carriers
 * when they view a load detail — no sensitive financial/tax data exposed.
 */
public record BrokerPublicDto(
        UUID id,
        String mcNumber,
        String dotNumber,
        String legalName,
        String companyName,
        String operatingStatus,
        String city,
        String state,
        String phoneNumber,
        String email,
        Integer ratingScore
) {}
