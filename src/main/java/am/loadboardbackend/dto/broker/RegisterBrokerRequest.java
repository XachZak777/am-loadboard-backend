package am.loadboardbackend.dto.broker;

public record RegisterBrokerRequest(
        String email,
        String password,
        String mcNumber
) {}