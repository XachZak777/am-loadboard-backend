package am.loadboardbackend.dto.auth;

public record RegisterBrokerFromPreviewRequest(
        String email,
        String password,
        String dotNumber,
        String mcNumber,
        String legalName,
        String operatingStatus,
        Boolean brokerAuthorityActive
) {}
