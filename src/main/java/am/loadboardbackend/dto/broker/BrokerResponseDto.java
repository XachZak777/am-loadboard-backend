package am.loadboardbackend.dto.broker;

import java.math.BigDecimal;

public record BrokerResponseDto(
        String mcNumber,
        String dotNumber,
        String legalName,
        String companyName,
        String operatingStatus,
        boolean brokerAuthorityActive,
        // contact
        String phoneNumber,
        String mailingAddress,
        String city,
        String state,
        String zipCode,
        // insurance
        String insuranceCompany,
        BigDecimal cargoInsurance,
        BigDecimal liabilityInsurance,
        // tax
        String taxIdType,
        String taxId,
        // bond
        String bondCompany,
        String bondPolicyNumber,
        String bondCoverage,
        String bondEffectiveDate,
        String bondAgentFirstName,
        String bondAgentLastName,
        String bondAgentEmail,
        String bondAgentPhone
) {}
