package am.loadboardbackend.dto.broker;

/**
 * Publicly shareable broker contact info returned to authenticated carriers
 * when they view a load detail — no sensitive financial/tax data exposed.
 */
public record BrokerPublicDto(
        String mcNumber,
        String dotNumber,
        String legalName,
        String companyName,
        String operatingStatus,
        String city,
        String state,
        String phoneNumber,
        String email
) {}
