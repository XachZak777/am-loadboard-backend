package am.loadboardbackend.service;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.RegisterAdminRequest;
import am.loadboardbackend.dto.dealer.RegisterDealerRequest;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.Dealer;
import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import am.loadboardbackend.repository.BrokerRepository;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.DealerRepository;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserRepository userRepository;
    private final CarrierRepository carrierRepository;
    private final BrokerRepository brokerRepository;
    private final DealerRepository dealerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Transactional
    public LoginResponse registerCarrierWithPreview(am.loadboardbackend.dto.auth.RegisterCarrierFromPreviewRequest req) {
        log.info("RegisterCarrierWithPreview start email={} mc={} dot={}", req.email(), req.mcNumber(), req.dotNumber());
        assertEmailNotTaken(req.email());
        Carrier carrier = new Carrier();
        carrier.setDotNumber(req.dotNumber());
        carrier.setMcNumber(req.mcNumber());
        carrier.setLegalName(req.legalName());
        carrier.setDbaName(req.dbaName());
        carrier.setOperatingStatus(req.operatingStatus());
        carrier.setVerified("Y".equalsIgnoreCase(req.allowedToOperate()));
        carrier.setPhyStreet(req.phyStreet());
        carrier.setPhyCity(req.phyCity());
        carrier.setPhyState(req.phyState());
        carrier.setPhyZip(req.phyZip());
        carrier.setPhyCountry(req.phyCountry());
        carrier.setTotalDrivers(req.totalDrivers());
        carrier.setTotalPowerUnits(req.totalPowerUnits());

        removeOrphanedCarrier(carrier.getDotNumber(), carrier.getMcNumber());
        // Guard: ensure this DOT/MC is not already registered as a Broker
        assertDotAndMcNotUsedByBroker(carrier.getDotNumber(), carrier.getMcNumber());
        carrierRepository.save(carrier);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_CARRIER);
        user.setCarrier(carrier);
        user.setEmailVerified(false);
        user.setAdminApproved(false);

        userRepository.save(user);
        log.info("RegisterCarrierWithPreview success email={} userId={} carrierId={}", user.getEmail(), user.getId(), carrier.getId());

    return new LoginResponse(
        jwtUtil.generateToken(user),
        user.getId().toString(),
        user.getEmail(),
        user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
        user.isAdminApproved()
    );
    }

    @Transactional
    public LoginResponse registerBrokerWithPreview(am.loadboardbackend.dto.auth.RegisterBrokerFromPreviewRequest req) {
        log.info("RegisterBrokerWithPreview start email={} mc={} dot={}", req.email(), req.mcNumber(), req.dotNumber());
        assertEmailNotTaken(req.email());
        Broker broker = new Broker();
        broker.setDotNumber(req.dotNumber());
        broker.setMcNumber(req.mcNumber());
        broker.setLegalName(req.legalName());
        broker.setOperatingStatus(req.operatingStatus());
        broker.setBrokerAuthorityActive(Boolean.TRUE.equals(req.brokerAuthorityActive()));

        removeOrphanedBroker(broker.getMcNumber());
        // Guard: ensure this MC/DOT is not already registered as a Carrier
        assertMcAndDotNotUsedByCarrier(broker.getDotNumber(), broker.getMcNumber());
        brokerRepository.save(broker);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_BROKER);
        user.setBroker(broker);
    user.setEmailVerified(false);
        user.setAdminApproved(false);

        userRepository.save(user);
        log.info("RegisterBrokerWithPreview success email={} userId={} brokerId={}", user.getEmail(), user.getId(), broker.getId());

    return new LoginResponse(
        jwtUtil.generateToken(user),
        user.getId().toString(),
        user.getEmail(),
        user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
        user.isAdminApproved()
    );
    }

    /**
     * Minimal broker registration — creates a User with ROLE_BROKER and a stub Broker
     * entity (placeholder MC number).  The real profile fields are filled in by the
     * subsequent PATCH /api/brokers/profile call from the registration wizard.
     * Creating the stub here guarantees profileId is always set in the admin view even
     * if the profile-update call fails (e.g. due to a network error or stale cookie).
     */
    @Transactional
    public LoginResponse registerBrokerMinimal(String email, String password) {
        log.info("RegisterBrokerMinimal start email={}", email);
        assertEmailNotTaken(email);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.ROLE_BROKER);
        user.setEmailVerified(false);
        user.setAdminApproved(false);
        userRepository.save(user);

        Broker broker = new Broker();
        broker.setMcNumber("PENDING-MC-" + user.getId());
        brokerRepository.save(broker);
        user.setBroker(broker);
        userRepository.save(user);

        log.info("RegisterBrokerMinimal success email={} userId={} brokerId={}", email, user.getId(), broker.getId());
        return authService.issueTokenForUser(user);
    }

    /**
     * Minimal carrier registration — creates a User with ROLE_CARRIER and a stub Carrier
     * entity (placeholder DOT/MC numbers).  The real profile fields are filled in by the
     * subsequent PATCH /api/carriers/profile call from the registration wizard.
     * Creating the stub here guarantees profileId is always set in the admin view even
     * if the profile-update call fails (e.g. due to a network error or stale cookie).
     */
    @Transactional
    public LoginResponse registerCarrierMinimal(String email, String password) {
        log.info("RegisterCarrierMinimal start email={}", email);
        assertEmailNotTaken(email);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.ROLE_CARRIER);
        user.setEmailVerified(false);
        user.setAdminApproved(false);
        userRepository.save(user);

        Carrier carrier = new Carrier();
        carrier.setDotNumber("PENDING-" + user.getId());
        carrier.setMcNumber("PENDING-MC-" + user.getId());
        carrierRepository.save(carrier);
        user.setCarrier(carrier);
        userRepository.save(user);

        log.info("RegisterCarrierMinimal success email={} userId={} carrierId={}", email, user.getId(), carrier.getId());
        return authService.issueTokenForUser(user);
    }

    @Transactional
    public LoginResponse registerCarrierFull(am.loadboardbackend.dto.auth.RegisterCarrierFullRequest req) {
        log.info("RegisterCarrierFull start email={} dot={}", req.email(), req.dotNumber());
        assertEmailNotTaken(req.email());

        String mcNumber = (req.mcNumber() != null && !req.mcNumber().isBlank())
                ? req.mcNumber()
                : "PENDING-MC-" + java.util.UUID.randomUUID();

        // Remove any orphaned carrier rows with the same DOT/MC so the unique
        // constraint does not block re-registration after an admin hard-delete.
        if (req.dotNumber() != null) {
            carrierRepository.findByDotNumber(req.dotNumber()).ifPresent(existing -> {
                boolean hasOwner = userRepository.findByCarrierId(existing.getId()).isPresent();
                if (!hasOwner) {
                    log.info("Removing orphaned Carrier id={} dot={}", existing.getId(), req.dotNumber());
                    carrierRepository.delete(existing);
                } else {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "DOT number " + req.dotNumber() + " is already registered to another carrier account.");
                }
            });
        }
        if (req.mcNumber() != null && !req.mcNumber().isBlank()) {
            carrierRepository.findByMcNumber(mcNumber).ifPresent(existing -> {
                boolean hasOwner = userRepository.findByCarrierId(existing.getId()).isPresent();
                if (!hasOwner) {
                    carrierRepository.delete(existing);
                } else {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "MC number " + mcNumber + " is already registered to another carrier account.");
                }
            });
        }
        assertDotAndMcNotUsedByBroker(req.dotNumber(), mcNumber);

        Carrier carrier = new Carrier();
        carrier.setDotNumber(req.dotNumber());
        carrier.setMcNumber(mcNumber);
        carrier.setCompanyName(req.companyName());
        carrier.setDbaName(req.dbaName());
        carrier.setPhoneNumber(req.phoneNumber());
        carrier.setInsuranceCompany(req.insuranceCompany());
        carrier.setCargoInsurance(req.cargoInsurance());
        carrier.setLiabilityInsurance(req.liabilityInsurance());
        carrier.setTaxIdType(req.taxIdType());
        carrier.setTaxId(req.taxId());
        carrier.setMailingAddress(req.mailingAddress());
        carrier.setCity(req.city());
        carrier.setState(req.state());
        carrier.setZipCode(req.zipCode());
        carrier.setPreferredLines(req.preferredLines());
        carrierRepository.save(carrier);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_CARRIER);
        user.setCarrier(carrier);
        user.setEmailVerified(false);
        user.setAdminApproved(false);
        userRepository.save(user);

        log.info("RegisterCarrierFull success email={} userId={} carrierId={}", user.getEmail(), user.getId(), carrier.getId());
        return authService.issueTokenForUser(user);
    }

    @Transactional
    public LoginResponse registerBrokerFull(am.loadboardbackend.dto.auth.RegisterBrokerFullRequest req) {
        log.info("RegisterBrokerFull start email={} mc={} dot={}", req.email(), req.mcNumber(), req.dotNumber());
        assertEmailNotTaken(req.email());

        if (req.dotNumber() != null && !req.dotNumber().isBlank()) {
            brokerRepository.findByDotNumber(req.dotNumber()).ifPresent(existing -> {
                boolean hasOwner = userRepository.findByBrokerId(existing.getId()).isPresent();
                if (!hasOwner) {
                    log.info("Removing orphaned Broker id={} dot={}", existing.getId(), req.dotNumber());
                    brokerRepository.delete(existing);
                } else {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "DOT number " + req.dotNumber() + " is already registered to another broker account.");
                }
            });
        }
        if (req.mcNumber() != null && !req.mcNumber().isBlank()) {
            brokerRepository.findByMcNumber(req.mcNumber()).ifPresent(existing -> {
                boolean hasOwner = userRepository.findByBrokerId(existing.getId()).isPresent();
                if (!hasOwner) {
                    log.info("Removing orphaned Broker id={} mc={}", existing.getId(), req.mcNumber());
                    brokerRepository.delete(existing);
                } else {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "MC number " + req.mcNumber() + " is already registered to another broker account.");
                }
            });
        }
        assertMcAndDotNotUsedByCarrier(req.dotNumber(), req.mcNumber());

        Broker broker = new Broker();
        broker.setDotNumber(req.dotNumber());
        broker.setMcNumber(req.mcNumber() != null && !req.mcNumber().isBlank()
                ? req.mcNumber() : "PENDING-MC-" + java.util.UUID.randomUUID());
        broker.setCompanyName(req.companyName());
        broker.setPhoneNumber(req.phoneNumber());
        broker.setTaxIdType(req.taxIdType());
        broker.setTaxId(req.taxId());
        broker.setMailingAddress(req.mailingAddress());
        broker.setCity(req.city());
        broker.setState(req.state());
        broker.setZipCode(req.zipCode());
        broker.setBondCompany(req.bondCompany());
        broker.setBondPolicyNumber(req.bondPolicyNumber());
        broker.setBondCoverage(req.bondCoverage());
        broker.setBondEffectiveDate(req.bondEffectiveDate());
        broker.setBondAgentFirstName(req.bondAgentFirstName());
        broker.setBondAgentLastName(req.bondAgentLastName());
        broker.setBondAgentEmail(req.bondAgentEmail());
        broker.setBondAgentPhone(req.bondAgentPhone());
        brokerRepository.save(broker);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_BROKER);
        user.setBroker(broker);
        user.setEmailVerified(false);
        user.setAdminApproved(false);
        userRepository.save(user);

        log.info("RegisterBrokerFull success email={} userId={} brokerId={}", user.getEmail(), user.getId(), broker.getId());
        return authService.issueTokenForUser(user);
    }

    @Transactional
    public LoginResponse registerAdmin(RegisterAdminRequest request) {
        // Check if any admin exists; if yes, require authentication
        long adminCount = userRepository.countByRole(UserRole.ROLE_ADMIN);
        if (adminCount > 0) {
            User currentUser = authService.currentUserOrThrow();
            if (currentUser.getRole() != UserRole.ROLE_ADMIN) {
                throw new RuntimeException("Only admins can create new admins");
            }
        }

        log.info("Registering admin email={}", request.getEmail());

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Create admin user
        User admin = new User();
        admin.setEmail(request.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(UserRole.ROLE_ADMIN);
    admin.setEmailVerified(true);
    admin.setEmailVerifiedAt(java.time.LocalDateTime.now());
        admin.setAdminApproved(true);
        admin.setAdminApprovedAt(java.time.LocalDateTime.now());

        User savedAdmin = userRepository.save(admin);
        log.info("Admin registered successfully email={} userId={}", request.getEmail(), savedAdmin.getId());

    return new LoginResponse(
        jwtUtil.generateToken(savedAdmin),
        savedAdmin.getId().toString(),
        savedAdmin.getEmail(),
        savedAdmin.getRole() != null ? savedAdmin.getRole().name().replace("ROLE_", "") : null,
        savedAdmin.isAdminApproved()
    );
    }

    @Transactional
    public LoginResponse registerDealer(RegisterDealerRequest req) {
        log.info("RegisterDealer start email={}", req.email());
        assertEmailNotTaken(req.email());

        Dealer dealer = new Dealer();
        dealer.setCompanyName(req.companyName());
        dealer.setOwnerFirstName(req.ownerFirstName());
        dealer.setOwnerLastName(req.ownerLastName());
        dealer.setBusinessPhone(req.businessPhone());
        dealer.setCompanyAddress(req.companyAddress());
        dealer.setCity(req.city());
        dealer.setState(req.state());
        dealer.setZipCode(req.zipCode());
        dealer.setYearEstablished(req.yearEstablished());
        dealer.setDealerLicenseNumber(req.dealerLicenseNumber());
        dealer.setAuctionAccessNumber(req.auctionAccessNumber());
        dealer.setHowDidYouHear(req.howDidYouHear());
        dealerRepository.save(dealer);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_DEALER);
        user.setDealer(dealer);
        user.setEmailVerified(false);
        user.setAdminApproved(false);
        userRepository.save(user);

        log.info("RegisterDealer success email={} userId={} dealerId={}", user.getEmail(), user.getId(), dealer.getId());
        return authService.issueTokenForUser(user);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Throws 409 Conflict if the email is already taken.
     */
    private void assertEmailNotTaken(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address is already in use");
        }
    }

    /**
     * If a Carrier row with the same DOT/MC exists but has NO associated user
     * (orphaned after an admin delete), remove it so the new registration can
     * create a fresh one.
     */
    private void removeOrphanedCarrier(String dotNumber, String mcNumber) {
        if (dotNumber != null) {
            carrierRepository.findByDotNumber(dotNumber).ifPresent(existing -> {
                boolean hasOwner = userRepository.findByCarrierId(existing.getId()).isPresent();
                if (!hasOwner) {
                    log.info("Removing orphaned Carrier id={} dot={}", existing.getId(), dotNumber);
                    carrierRepository.delete(existing);
                }
            });
        }
        if (mcNumber != null) {
            carrierRepository.findByMcNumber(mcNumber).ifPresent(existing -> {
                boolean hasOwner = userRepository.findByCarrierId(existing.getId()).isPresent();
                if (!hasOwner) {
                    log.info("Removing orphaned Carrier id={} mc={}", existing.getId(), mcNumber);
                    carrierRepository.delete(existing);
                }
            });
        }
    }

    /**
     * Same as {@link #removeOrphanedCarrier} but for Broker rows.
     */
    private void removeOrphanedBroker(String mcNumber) {
        if (mcNumber != null) {
            brokerRepository.findByMcNumber(mcNumber).ifPresent(existing -> {
                boolean hasOwner = userRepository.findByBrokerId(existing.getId()).isPresent();
                if (!hasOwner) {
                    log.info("Removing orphaned Broker id={} mc={}", existing.getId(), mcNumber);
                    brokerRepository.delete(existing);
                }
            });
        }
    }

    /**
     * Throws 409 Conflict if the DOT or MC number is already registered under a Broker account.
     * Prevents a carrier from hijacking a broker's MC/DOT and vice-versa.
     */
    private void assertDotAndMcNotUsedByBroker(String dotNumber, String mcNumber) {
        if (dotNumber != null) {
            brokerRepository.findByDotNumber(dotNumber).ifPresent(b -> {
                boolean hasOwner = userRepository.findByBrokerId(b.getId()).isPresent();
                if (hasOwner) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "DOT number " + dotNumber + " is already registered under a Broker account. " +
                            "Please use the Broker registration flow.");
                }
            });
        }
        if (mcNumber != null) {
            brokerRepository.findByMcNumber(mcNumber).ifPresent(b -> {
                boolean hasOwner = userRepository.findByBrokerId(b.getId()).isPresent();
                if (hasOwner) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "MC number " + mcNumber + " is already registered under a Broker account. " +
                            "Please use the Broker registration flow.");
                }
            });
        }
    }

    /**
     * Throws 409 Conflict if the DOT or MC number is already registered under a Carrier account.
     * Prevents a broker from hijacking a carrier's MC/DOT.
     */
    private void assertMcAndDotNotUsedByCarrier(String dotNumber, String mcNumber) {
        if (dotNumber != null) {
            carrierRepository.findByDotNumber(dotNumber).ifPresent(c -> {
                boolean hasOwner = userRepository.findByCarrierId(c.getId()).isPresent();
                if (hasOwner) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "DOT number " + dotNumber + " is already registered under a Carrier account. " +
                            "Please use the Carrier registration flow.");
                }
            });
        }
        if (mcNumber != null) {
            carrierRepository.findByMcNumber(mcNumber).ifPresent(c -> {
                boolean hasOwner = userRepository.findByCarrierId(c.getId()).isPresent();
                if (hasOwner) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "MC number " + mcNumber + " is already registered under a Carrier account. " +
                            "Please use the Carrier registration flow.");
                }
            });
        }
    }
}
