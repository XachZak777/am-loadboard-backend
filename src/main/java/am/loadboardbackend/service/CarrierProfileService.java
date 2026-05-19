package am.loadboardbackend.service;

import am.loadboardbackend.dto.carrier.CarrierProfileRequest;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarrierProfileService {

    private final CarrierRepository carrierRepository;
    private final UserRepository userRepository;

    /**
     * Upsert carrier profile for the authenticated user.
     * Only non-null fields in the request are applied (partial update).
     * Sets adminApproved = false on the user after saving (requires admin review).
     */
    @Transactional
    public void updateProfile(User user, CarrierProfileRequest req) {
        Carrier carrier = resolveCarrier(user);

        boolean postRegistration = user.isAdminApproved();

        // Locked fields — only writable during registration wizard (before admin approval)
        if (!postRegistration) {
            if (req.companyName() != null)    carrier.setCompanyName(req.companyName());
            if (req.dotNumber() != null)      carrier.setDotNumber(req.dotNumber());
            if (req.mcNumber() != null)       carrier.setMcNumber(req.mcNumber());
            if (req.mailingAddress() != null) carrier.setMailingAddress(req.mailingAddress());
            if (req.city() != null)           carrier.setCity(req.city());
            if (req.state() != null)          carrier.setState(req.state());
            if (req.zipCode() != null)        carrier.setZipCode(req.zipCode());
        }

        // Always editable
        if (req.phoneNumber() != null)        carrier.setPhoneNumber(req.phoneNumber());
        if (req.insuranceCompany() != null)   carrier.setInsuranceCompany(req.insuranceCompany());
        if (req.cargoInsurance() != null)     carrier.setCargoInsurance(req.cargoInsurance());
        if (req.liabilityInsurance() != null) carrier.setLiabilityInsurance(req.liabilityInsurance());
        if (req.taxIdType() != null)          carrier.setTaxIdType(req.taxIdType());
        if (req.taxId() != null)              carrier.setTaxId(req.taxId());
        if (req.preferredLines() != null)     carrier.setPreferredLines(req.preferredLines());

        carrierRepository.save(carrier);

        // Ensure carrier is linked to the user
        if (user.getCarrier() == null) {
            user.setCarrier(carrier);
        }

        // Only pending-approval users need re-review after profile changes
        if (!postRegistration) {
            user.setAdminApproved(false);
        }
        userRepository.save(user);

        log.info("CarrierProfile updated userId={} carrierId={}", user.getId(), carrier.getId());
    }

    /**
     * Determine whether the carrier profile is considered "complete".
     * Required fields: companyName, phoneNumber, insuranceCompany,
     * cargoInsurance, liabilityInsurance, taxIdType, taxId.
     */
    public boolean isProfileComplete(User user) {
        Carrier carrier = user.getCarrier();
        if (carrier == null) return false;

        return carrier.getCompanyName() != null
                && carrier.getPhoneNumber() != null
                && carrier.getInsuranceCompany() != null
                && carrier.getCargoInsurance() != null
                && carrier.getLiabilityInsurance() != null
                && carrier.getTaxIdType() != null
                && carrier.getTaxId() != null;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Carrier resolveCarrier(User user) {
        if (user.getCarrier() != null) {
            return carrierRepository.findById(user.getCarrier().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Carrier record not found"));
        }
        // Create a stub carrier if one does not exist yet
        log.info("No existing carrier for userId={}; creating stub", user.getId());
        Carrier carrier = new Carrier();
        // Provide a placeholder dotNumber to satisfy the non-null unique constraint;
        // the client is expected to supply the real value in the request body.
        carrier.setDotNumber("PENDING-" + user.getId());
        carrier.setMcNumber("PENDING-MC-" + user.getId());
        return carrierRepository.save(carrier);
    }
}
