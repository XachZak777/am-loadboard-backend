package am.loadboardbackend.service;

import am.loadboardbackend.dto.broker.BrokerProfileRequest;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.BrokerRepository;
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
public class BrokerProfileService {

    private final BrokerRepository brokerRepository;
    private final UserRepository userRepository;

    /**
     * Upsert broker profile for the authenticated user.
     * Only non-null fields in the request are applied (partial update).
     * Sets adminApproved = false on the user after saving (requires admin review).
     */
    @Transactional
    public void updateProfile(User user, BrokerProfileRequest req) {
        Broker broker = resolveBroker(user);

        // Partial update — only overwrite non-null fields
        if (req.companyName() != null)        broker.setCompanyName(req.companyName());
        if (req.dotNumber() != null)          broker.setDotNumber(req.dotNumber());
        if (req.mcNumber() != null)           broker.setMcNumber(req.mcNumber());
        if (req.phoneNumber() != null)        broker.setPhoneNumber(req.phoneNumber());
        if (req.insuranceCompany() != null)   broker.setInsuranceCompany(req.insuranceCompany());
        if (req.cargoInsurance() != null)     broker.setCargoInsurance(req.cargoInsurance());
        if (req.liabilityInsurance() != null) broker.setLiabilityInsurance(req.liabilityInsurance());
        if (req.taxIdType() != null)          broker.setTaxIdType(req.taxIdType());
        if (req.taxId() != null)              broker.setTaxId(req.taxId());
        if (req.mailingAddress() != null)     broker.setMailingAddress(req.mailingAddress());
        if (req.city() != null)               broker.setCity(req.city());
        if (req.state() != null)              broker.setState(req.state());
        if (req.zipCode() != null)            broker.setZipCode(req.zipCode());

        brokerRepository.save(broker);

        // Ensure broker is linked to the user
        if (user.getBroker() == null) {
            user.setBroker(broker);
        }

        // New/updated profiles require admin approval
        user.setAdminApproved(false);
        userRepository.save(user);

        log.info("BrokerProfile updated userId={} brokerId={}", user.getId(), broker.getId());
    }

    /**
     * Determine whether the broker profile is considered "complete".
     * Required fields: companyName, phoneNumber, insuranceCompany,
     * cargoInsurance, liabilityInsurance, taxIdType, taxId.
     */
    public boolean isProfileComplete(User user) {
        Broker broker = user.getBroker();
        if (broker == null) return false;

        return broker.getCompanyName() != null
                && broker.getPhoneNumber() != null
                && broker.getInsuranceCompany() != null
                && broker.getCargoInsurance() != null
                && broker.getLiabilityInsurance() != null
                && broker.getTaxIdType() != null
                && broker.getTaxId() != null;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Broker resolveBroker(User user) {
        if (user.getBroker() != null) {
            return brokerRepository.findById(user.getBroker().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Broker record not found"));
        }
        // Create a stub broker if one does not exist yet
        log.info("No existing broker for userId={}; creating stub", user.getId());
        Broker broker = new Broker();
        broker.setMcNumber("PENDING-MC-" + user.getId());
        return brokerRepository.save(broker);
    }
}
