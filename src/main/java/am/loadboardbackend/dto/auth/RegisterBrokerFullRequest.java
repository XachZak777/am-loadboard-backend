package am.loadboardbackend.dto.auth;

public record RegisterBrokerFullRequest(
        String email,
        String password,
        String companyName,
        String dotNumber,
        String mcNumber,
        String phoneNumber,
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
