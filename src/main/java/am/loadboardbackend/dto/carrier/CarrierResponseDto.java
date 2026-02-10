package am.loadboardbackend.dto.carrier;

import java.util.UUID;

public record CarrierResponseDto(
        UUID id,
        String dotNumber,
        String mcNumber,
        String legalName,
        String dbaName,
        String operatingStatus,
        String safetyRating,
        boolean verified,
        String phyCity,
        String phyState,
        Integer totalDrivers,
        Integer totalPowerUnits
) {}
