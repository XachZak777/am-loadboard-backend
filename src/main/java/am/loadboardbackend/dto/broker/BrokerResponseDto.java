package am.loadboardbackend.dto.broker;

public record BrokerResponseDto (
        String mcNumber,
        String dotNumber,
        String legalName,
        String operatingStatus,
        boolean brokerAuthorityActive
) {}
