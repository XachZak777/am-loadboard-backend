package am.loadboardbackend.dto.broker;

import java.math.BigDecimal;

/**
 * Request body for PATCH /api/brokers/profile.
 * All fields are optional — only non-null values will be applied (partial update).
 */
public record BrokerProfileRequest(
        String companyName,
        String dotNumber,
        String mcNumber,
        String phoneNumber,
        String insuranceCompany,
        BigDecimal cargoInsurance,
        BigDecimal liabilityInsurance,
        String taxIdType,
        String taxId,
        String mailingAddress,
        String city,
        String state,
        String zipCode,
        String bondCompany,
        String bondPolicyNumber,
        String bondCoverage,
        String bondEffectiveDate,
        String bondAgentFirstName,
        String bondAgentLastName,
        String bondAgentEmail,
        String bondAgentPhone
) {}
