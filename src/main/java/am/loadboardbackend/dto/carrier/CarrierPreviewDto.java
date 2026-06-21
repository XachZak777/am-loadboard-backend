package am.loadboardbackend.dto.carrier;

public record CarrierPreviewDto(
        String dotNumber,
        String mcNumber,
        String legalName,
        String dbaName,
        String phyCity,
        String phyState
) {}
