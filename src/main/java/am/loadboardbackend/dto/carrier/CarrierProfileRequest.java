package am.loadboardbackend.dto.carrier;

import java.math.BigDecimal;

/**
 * Request body for PATCH /api/carriers/profile.
 * All fields are optional — only non-null values will be applied (partial update).
 */
public record CarrierProfileRequest(
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
        String zipCode
) {}
