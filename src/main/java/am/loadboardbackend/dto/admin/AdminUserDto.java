package am.loadboardbackend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full view of a registered user for the admin panel.
 * Includes the linked carrier/broker profile fields and uploaded documents.
 */
public record AdminUserDto(

        // ── User identity ────────────────────────────────────────────────
        UUID    userId,
        String  email,
        String  role,
        boolean adminApproved,
        LocalDateTime adminApprovedAt,
        boolean emailVerified,
        boolean declined,
        LocalDateTime declinedAt,
        LocalDateTime createdAt,

        // ── Profile (carrier or broker) ──────────────────────────────────
        UUID    profileId,
        String  companyName,
        String  dotNumber,
        String  mcNumber,
        String  phoneNumber,
        String  mailingAddress,
        String  city,
        String  state,
        String  zipCode,
        String  insuranceCompany,
        BigDecimal cargoInsurance,
        BigDecimal liabilityInsurance,
        String  taxIdType,
        String  taxId,

        // ── Carrier-only fields ──────────────────────────────────────────
        String  dbaName,
        String  preferredLines,

        // ── Bond fields (broker only) ────────────────────────────────────
        String  bondCompany,
        String  bondPolicyNumber,
        String  bondCoverage,
        String  bondEffectiveDate,
        String  bondAgentFirstName,
        String  bondAgentLastName,
        String  bondAgentEmail,
        String  bondAgentPhone,

        // ── Dealer-only fields ───────────────────────────────────────────
        String  ownerFirstName,
        String  ownerLastName,
        String  yearEstablished,
        String  dealerLicenseNumber,
        String  auctionAccessNumber,
        String  howDidYouHear,

        // ── Uploaded documents (W9 etc.) ─────────────────────────────────
        List<AdminDocumentDto> documents
) {}
