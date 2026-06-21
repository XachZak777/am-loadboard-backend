package am.loadboardbackend.dto.auth;

public record RegisterCarrierFromPreviewRequest(
        String email,
        String password,
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
        Integer totalPowerUnits
) {}
