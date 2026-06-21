package am.loadboardbackend.dto.auth;

import java.math.BigDecimal;

public record RegisterCarrierFullRequest(
        String email,
        String password,
        String companyName,
        String dbaName,
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
        String preferredLines
) {}
