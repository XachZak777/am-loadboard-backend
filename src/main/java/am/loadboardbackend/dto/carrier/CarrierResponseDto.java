package am.loadboardbackend.dto.carrier;

import java.math.BigDecimal;
import java.util.UUID;

public record CarrierResponseDto(
        UUID id,
        String dotNumber,
        String mcNumber,
        String legalName,
        String dbaName,
        String companyName,
        String operatingStatus,
        String safetyRating,
        boolean verified,
        String phyCity,
        String phyState,
        Integer totalDrivers,
        Integer totalPowerUnits,
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
        String preferredLines
) {}
