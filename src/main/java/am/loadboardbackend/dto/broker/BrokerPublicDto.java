package am.loadboardbackend.dto.broker;

import java.util.UUID;

/**
 * Publicly shareable broker contact info returned to authenticated carriers.
 * Bond policy number, coverage amount, and effective date are intentionally
 * excluded — only bond company and agent contact are exposed.
 */
public record BrokerPublicDto(
        UUID id,
        String mcNumber,
        String dotNumber,
        String legalName,
        String companyName,
        String operatingStatus,
        String mailingAddress,
        String city,
        String state,
        String zipCode,
        String phoneNumber,
        String email,
        Integer ratingScore,
        // Bond — company and agent contact only (policy/coverage/dates are private)
        String bondCompany,
        String bondAgentFirstName,
        String bondAgentLastName,
        String bondAgentPhone
) {}
