package am.loadboardbackend.service;

import am.loadboardbackend.dto.admin.AdminDocumentDto;
import am.loadboardbackend.dto.admin.AdminUserDto;
import am.loadboardbackend.model.*;
import am.loadboardbackend.repository.AuditLogRepository;
import am.loadboardbackend.repository.DocumentRepository;
import am.loadboardbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository       userRepository;
    private final DocumentRepository   documentRepository;
    private final AuditLogRepository   auditLogRepository;
    private final AuthService          authService;

    /**
     * Returns all non-admin users (carriers + brokers) with their profile
     * details and uploaded documents.
     */
    public List<AdminUserDto> getAllUsers() {
        log.info("Admin fetching all users");
        return userRepository.findByRoleNot(UserRole.ROLE_ADMIN)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Returns a single user's full detail.
     */
    public AdminUserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toDto(user);
    }

    /**
     * Hard-deletes a user and their linked carrier or broker record.
     * Also removes their documents and writes an audit entry.
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User admin = authService.currentUserOrThrow();
        User user  = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        log.info("Admin {} deleting user {} ({})", admin.getEmail(), userId, user.getEmail());

        // Remove documents
        String ownerType = user.getCarrier() != null ? "CARRIER" : "BROKER";
        UUID   ownerId   = user.getCarrier() != null
                ? user.getCarrier().getId()
                : (user.getBroker() != null ? user.getBroker().getId() : null);
        if (ownerId != null) {
            List<Document> docs = documentRepository.findByOwnerIdAndOwnerType(ownerId, ownerType);
            documentRepository.deleteAll(docs);
        }

        auditLogRepository.save(AuditLog.builder()
                .user(admin)
                .action("DELETE_USER")
                .entityType("USER")
                .entityId(userId)
                .details("Deleted user: " + user.getEmail() + " role=" + user.getRole())
                .build());

        // Deleting the user cascades to carrier/broker via FK (User owns the FK)
        userRepository.delete(user);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private AdminUserDto toDto(User user) {
        UUID    profileId         = null;
        String  companyName       = null;
        String  dotNumber         = null;
        String  mcNumber          = null;
        String  phoneNumber       = null;
        String  mailingAddress    = null;
        String  city              = null;
        String  state             = null;
        String  zipCode           = null;
        String  insuranceCompany  = null;
        String  cargoInsurance    = null;
        String  liabilityInsurance= null;
        String  taxIdType         = null;
        String  taxId             = null;
        String  ownerType         = null;

        if (user.getCarrier() != null) {
            Carrier c = user.getCarrier();
            profileId          = c.getId();
            companyName        = c.getCompanyName() != null ? c.getCompanyName() : c.getLegalName();
            dotNumber          = c.getDotNumber();
            mcNumber           = c.getMcNumber();
            phoneNumber        = c.getPhoneNumber();
            mailingAddress     = c.getMailingAddress();
            city               = c.getCity();
            state              = c.getState();
            zipCode            = c.getZipCode();
            insuranceCompany   = c.getInsuranceCompany();
            cargoInsurance     = c.getCargoInsurance() != null ? c.getCargoInsurance().toPlainString() : null;
            liabilityInsurance = c.getLiabilityInsurance() != null ? c.getLiabilityInsurance().toPlainString() : null;
            taxIdType          = c.getTaxIdType();
            taxId              = c.getTaxId();
            ownerType          = "CARRIER";
        } else if (user.getBroker() != null) {
            Broker b = user.getBroker();
            profileId          = b.getId();
            companyName        = b.getCompanyName() != null ? b.getCompanyName() : b.getLegalName();
            dotNumber          = b.getDotNumber();
            mcNumber           = b.getMcNumber();
            phoneNumber        = b.getPhoneNumber();
            mailingAddress     = b.getMailingAddress();
            city               = b.getCity();
            state              = b.getState();
            zipCode            = b.getZipCode();
            insuranceCompany   = b.getInsuranceCompany();
            cargoInsurance     = b.getCargoInsurance() != null ? b.getCargoInsurance().toPlainString() : null;
            liabilityInsurance = b.getLiabilityInsurance() != null ? b.getLiabilityInsurance().toPlainString() : null;
            taxIdType          = b.getTaxIdType();
            taxId              = b.getTaxId();
            ownerType          = "BROKER";
        }

        List<AdminDocumentDto> documents = (profileId != null && ownerType != null)
                ? documentRepository.findByOwnerIdAndOwnerType(profileId, ownerType)
                        .stream()
                        .map(d -> new AdminDocumentDto(
                                d.getId(),
                                d.getDocumentType(),
                                d.getOriginalName(),
                                d.getFileUrl(),
                                d.getUploadedAt()))
                        .toList()
                : List.of();

        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
                user.isAdminApproved(),
                user.getAdminApprovedAt(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                profileId,
                companyName,
                dotNumber,
                mcNumber,
                phoneNumber,
                mailingAddress,
                city,
                state,
                zipCode,
                insuranceCompany,
                cargoInsurance,
                liabilityInsurance,
                taxIdType,
                taxId,
                documents
        );
    }
}
