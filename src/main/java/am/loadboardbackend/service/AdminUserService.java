package am.loadboardbackend.service;

import am.loadboardbackend.dto.admin.AdminDocumentDto;
import am.loadboardbackend.dto.admin.AdminUserDto;
import am.loadboardbackend.model.*;
import am.loadboardbackend.repository.AuditLogRepository;
import am.loadboardbackend.repository.BrokerRepository;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.DealerRepository;
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
    private final CarrierRepository    carrierRepository;
    private final BrokerRepository     brokerRepository;
    private final DealerRepository     dealerRepository;

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

    /** Returns only users who have been approved (adminApproved = true). */
    public List<AdminUserDto> getApprovedUsers() {
        log.info("Admin fetching approved users");
        return userRepository.findByRoleNotAndAdminApprovedTrue(UserRole.ROLE_ADMIN)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Returns users who have not yet been reviewed (adminApproved = false AND declined = false). */
    public List<AdminUserDto> getPendingUsers() {
        log.info("Admin fetching pending users");
        return userRepository.findByRoleNotAndAdminApprovedFalseAndDeclinedFalse(UserRole.ROLE_ADMIN)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Returns users whose registration was actively rejected (declined = true). */
    public List<AdminUserDto> getRejectedUsers() {
        log.info("Admin fetching rejected users");
        return userRepository.findByRoleNotAndDeclinedTrue(UserRole.ROLE_ADMIN)
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
        UUID    profileId          = null;
        String  companyName        = null;
        String  dotNumber          = null;
        String  mcNumber           = null;
        String  phoneNumber        = null;
        String  mailingAddress     = null;
        String  city               = null;
        String  state              = null;
        String  zipCode            = null;
        String  insuranceCompany   = null;
        java.math.BigDecimal cargoInsurance     = null;
        java.math.BigDecimal liabilityInsurance = null;
        String  taxIdType          = null;
        String  taxId              = null;
        String  ownerType          = null;
        String  dbaName            = null;
        String  preferredLines     = null;
        String  bondCompany        = null;
        String  bondPolicyNumber   = null;
        String  bondCoverage       = null;
        String  bondEffectiveDate  = null;
        String  bondAgentFirstName = null;
        String  bondAgentLastName  = null;
        String  bondAgentEmail     = null;
        String  bondAgentPhone     = null;
        String  ownerFirstName     = null;
        String  ownerLastName      = null;
        String  yearEstablished    = null;
        String  dealerLicenseNumber = null;
        String  auctionAccessNumber = null;
        String  howDidYouHear      = null;

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
            cargoInsurance     = c.getCargoInsurance();
            liabilityInsurance = c.getLiabilityInsurance();
            taxIdType          = c.getTaxIdType();
            taxId              = c.getTaxId();
            dbaName            = c.getDbaName();
            preferredLines     = c.getPreferredLines();
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
            cargoInsurance     = b.getCargoInsurance();
            liabilityInsurance = b.getLiabilityInsurance();
            taxIdType          = b.getTaxIdType();
            taxId              = b.getTaxId();
            bondCompany        = b.getBondCompany();
            bondPolicyNumber   = b.getBondPolicyNumber();
            bondCoverage       = b.getBondCoverage();
            bondEffectiveDate  = b.getBondEffectiveDate();
            bondAgentFirstName = b.getBondAgentFirstName();
            bondAgentLastName  = b.getBondAgentLastName();
            bondAgentEmail     = b.getBondAgentEmail();
            bondAgentPhone     = b.getBondAgentPhone();
            ownerType          = "BROKER";
        } else if (user.getDealer() != null) {
            am.loadboardbackend.model.Dealer d = user.getDealer();
            profileId           = d.getId();
            companyName         = d.getCompanyName();
            phoneNumber         = d.getBusinessPhone();
            mailingAddress      = d.getCompanyAddress();
            city                = d.getCity();
            state               = d.getState();
            zipCode             = d.getZipCode();
            ownerFirstName      = d.getOwnerFirstName();
            ownerLastName       = d.getOwnerLastName();
            yearEstablished     = d.getYearEstablished();
            dealerLicenseNumber = d.getDealerLicenseNumber();
            auctionAccessNumber = d.getAuctionAccessNumber();
            howDidYouHear       = d.getHowDidYouHear();
            ownerType           = "DEALER";
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
                user.isDeclined(),
                user.getDeclinedAt(),
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
                dbaName,
                preferredLines,
                bondCompany,
                bondPolicyNumber,
                bondCoverage,
                bondEffectiveDate,
                bondAgentFirstName,
                bondAgentLastName,
                bondAgentEmail,
                bondAgentPhone,
                ownerFirstName,
                ownerLastName,
                yearEstablished,
                dealerLicenseNumber,
                auctionAccessNumber,
                howDidYouHear,
                documents
        );
    }

    @Transactional
    public void updateCarrierProfile(UUID carrierId, am.loadboardbackend.dto.carrier.CarrierProfileRequest req) {
        am.loadboardbackend.model.Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrier not found"));
        if (req.companyName() != null)        carrier.setCompanyName(req.companyName());
        if (req.dotNumber() != null)          carrier.setDotNumber(req.dotNumber());
        if (req.mcNumber() != null)           carrier.setMcNumber(req.mcNumber());
        if (req.phoneNumber() != null)        carrier.setPhoneNumber(req.phoneNumber());
        if (req.dbaName() != null)            carrier.setDbaName(req.dbaName());
        if (req.insuranceCompany() != null)   carrier.setInsuranceCompany(req.insuranceCompany());
        if (req.cargoInsurance() != null)     carrier.setCargoInsurance(req.cargoInsurance());
        if (req.liabilityInsurance() != null) carrier.setLiabilityInsurance(req.liabilityInsurance());
        if (req.taxIdType() != null)          carrier.setTaxIdType(req.taxIdType());
        if (req.taxId() != null)              carrier.setTaxId(req.taxId());
        if (req.mailingAddress() != null)     carrier.setMailingAddress(req.mailingAddress());
        if (req.city() != null)               carrier.setCity(req.city());
        if (req.state() != null)              carrier.setState(req.state());
        if (req.zipCode() != null)            carrier.setZipCode(req.zipCode());
        if (req.preferredLines() != null)     carrier.setPreferredLines(req.preferredLines());
        carrierRepository.save(carrier);
        log.info("Admin updated carrier profile carrierId={}", carrierId);
    }

    @Transactional
    public void updateBrokerProfile(UUID brokerId, am.loadboardbackend.dto.broker.BrokerProfileRequest req) {
        am.loadboardbackend.model.Broker broker = brokerRepository.findById(brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broker not found"));
        if (req.companyName() != null)        broker.setCompanyName(req.companyName());
        if (req.dotNumber() != null)          broker.setDotNumber(req.dotNumber());
        if (req.mcNumber() != null)           broker.setMcNumber(req.mcNumber());
        if (req.phoneNumber() != null)        broker.setPhoneNumber(req.phoneNumber());
        if (req.taxIdType() != null)          broker.setTaxIdType(req.taxIdType());
        if (req.taxId() != null)              broker.setTaxId(req.taxId());
        if (req.mailingAddress() != null)     broker.setMailingAddress(req.mailingAddress());
        if (req.city() != null)               broker.setCity(req.city());
        if (req.state() != null)              broker.setState(req.state());
        if (req.zipCode() != null)            broker.setZipCode(req.zipCode());
        if (req.bondCompany() != null)        broker.setBondCompany(req.bondCompany());
        if (req.bondPolicyNumber() != null)   broker.setBondPolicyNumber(req.bondPolicyNumber());
        if (req.bondCoverage() != null)       broker.setBondCoverage(req.bondCoverage());
        if (req.bondEffectiveDate() != null)  broker.setBondEffectiveDate(req.bondEffectiveDate());
        if (req.bondAgentFirstName() != null) broker.setBondAgentFirstName(req.bondAgentFirstName());
        if (req.bondAgentLastName() != null)  broker.setBondAgentLastName(req.bondAgentLastName());
        if (req.bondAgentEmail() != null)     broker.setBondAgentEmail(req.bondAgentEmail());
        if (req.bondAgentPhone() != null)     broker.setBondAgentPhone(req.bondAgentPhone());
        brokerRepository.save(broker);
        log.info("Admin updated broker profile brokerId={}", brokerId);
    }
}
