package am.loadboardbackend.dto;

public record CarrierResponseDto(
        Long dotNumber,
        String mcNumber,
        String legalName,
        String operatingStatus,
        String safetyRating,
        boolean verified,
        FmcsaCarrier rawData
) {}
