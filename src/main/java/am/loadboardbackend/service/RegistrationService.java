package am.loadboardbackend.service;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.RegisterCarrierRequest;
import am.loadboardbackend.dto.auth.RegisterAdminRequest;
import am.loadboardbackend.dto.broker.RegisterBrokerRequest;
import am.loadboardbackend.dto.dealer.RegisterDealerRequest;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.CarrierValidation;
import am.loadboardbackend.model.BrokerValidation;
import am.loadboardbackend.model.Dealer;
import am.loadboardbackend.repository.CarrierValidationRepository;
import am.loadboardbackend.repository.BrokerValidationRepository;
import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import am.loadboardbackend.repository.BrokerRepository;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.DealerRepository;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.security.JwtUtil;
import am.loadboardbackend.service.validation.BrokerValidationResult;
import am.loadboardbackend.service.validation.CarrierValidationResult;
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
    private final CarrierValidationService carrierValidationService;
    private final BrokerValidationService brokerValidationService;
    private final CarrierValidationRepository carrierValidationRepo;
    private final BrokerValidationRepository brokerValidationRepo;
    private final TemporaryValidationStore tempStore;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Transactional
    public LoginResponse registerCarrier(RegisterCarrierRequest req) {
        log.info("RegisterCarrier start lookupValue={} lookupType={} email={}", req.lookupValue(), req.lookupType(), req.email());
        assertEmailNotTaken(req.email());

        CarrierValidationResult validation =
                carrierValidationService.validate(req.lookupValue(), req.lookupType());

        // Guard: ensure this DOT/MC is not already registered as a Broker
        assertDotAndMcNotUsedByBroker(validation.getDotNumber(), validation.getMcNumber());

        Carrier carrier = new Carrier();
        carrier.setDotNumber(validation.getDotNumber());
        carrier.setMcNumber(validation.getMcNumber());
        carrier.setLegalName(validation.getLegalName());
        carrier.setDbaName(validation.getDbaName());
        carrier.setOperatingStatus(validation.getOperatingStatus());
        carrier.setVerified(
                "Y".equalsIgnoreCase(validation.getAllowedToOperate())
        );
        carrier.setPhyStreet(validation.getPhyStreet());
        carrier.setPhyCity(validation.getPhyCity());
        carrier.setPhyState(validation.getPhyState());
        carrier.setPhyZip(validation.getPhyZip());
        carrier.setPhyCountry(validation.getPhyCountry());
        carrier.setTotalDrivers(validation.getTotalDrivers());
        carrier.setTotalPowerUnits(validation.getTotalPowerUnits());

        removeOrphanedCarrier(carrier.getDotNumber(), carrier.getMcNumber());
    carrierRepository.save(carrier);
    log.info("Carrier entity persisted id={} mc={} dot={}", carrier.getId(), carrier.getMcNumber(), carrier.getDotNumber());

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_CARRIER);
        user.setCarrier(carrier);
    user.setEmailVerified(false);
        user.setAdminApproved(false);

        userRepository.save(user);
        log.info("RegisterCarrier success email={} userId={} carrierId={}", user.getEmail(), user.getId(), carrier.getId());

    return new LoginResponse(
        jwtUtil.generateToken(user),
        user.getId().toString(),
        user.getEmail(),
        user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
        user.isAdminApproved()
    );
    }

    @Transactional
    public LoginResponse registerCarrierFromValidation(java.util.UUID validationId, String email, String password) {
        log.info("RegisterCarrierFromValidation start validationId={} email={}", validationId, email);
        assertEmailNotTaken(email);
            boolean usedCache = true;
            var cached = tempStore.getValidation(validationId);

            Carrier carrier = new Carrier();
            if (cached != null) {
                log.info("Registering carrier from cache id={}", validationId);
                carrier.setDotNumber(cached.dotNumber);
                carrier.setMcNumber(cached.mcNumber);
                carrier.setLegalName(cached.legalName);
                carrier.setDbaName(cached.dbaName);
                carrier.setOperatingStatus(cached.operatingStatus);
                carrier.setVerified("Y".equalsIgnoreCase(cached.allowedToOperate));
                carrier.setPhyStreet(cached.phyStreet);
                carrier.setPhyCity(cached.phyCity);
                carrier.setPhyState(cached.phyState);
                carrier.setPhyZip(cached.phyZip);
                carrier.setPhyCountry(cached.phyCountry);
                carrier.setTotalDrivers(cached.totalDrivers);
                carrier.setTotalPowerUnits(cached.totalPowerUnits);
            } else {
                usedCache = false;
                log.info("Cache miss for carrier validation id={}; falling back to DB", validationId);
                CarrierValidation cv = carrierValidationRepo.findById(validationId).orElseThrow();
                carrier.setDotNumber(cv.getDotNumber());
                carrier.setMcNumber(cv.getMcNumber());
                carrier.setLegalName(cv.getLegalName());
                carrier.setDbaName(cv.getDbaName());
                carrier.setOperatingStatus(cv.getOperatingStatus());
                carrier.setVerified("Y".equalsIgnoreCase(cv.getAllowedToOperate()));
                carrier.setPhyStreet(cv.getPhyStreet());
                carrier.setPhyCity(cv.getPhyCity());
                carrier.setPhyState(cv.getPhyState());
                carrier.setPhyZip(cv.getPhyZip());
                carrier.setPhyCountry(cv.getPhyCountry());
                carrier.setTotalDrivers(cv.getTotalDrivers());
                carrier.setTotalPowerUnits(cv.getTotalPowerUnits());
            }

            removeOrphanedCarrier(carrier.getDotNumber(), carrier.getMcNumber());
            // Guard: ensure this DOT/MC is not already registered as a Broker
            assertDotAndMcNotUsedByBroker(carrier.getDotNumber(), carrier.getMcNumber());
            carrierRepository.save(carrier);

            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(UserRole.ROLE_CARRIER);
            user.setCarrier(carrier);
            user.setEmailVerified(false);
            user.setAdminApproved(false);

        userRepository.save(user);
        log.info("RegisterCarrierFromValidation success validationId={} email={} userId={} carrierId={} cacheEvicted={}", validationId, user.getEmail(), user.getId(), carrier.getId(), usedCache);

    return new LoginResponse(
        jwtUtil.generateToken(user),
        user.getId().toString(),
        user.getEmail(),
        user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
        user.isAdminApproved()
    );
    }

    @Transactional
    public LoginResponse registerBrokerFromValidation(java.util.UUID validationId, String email, String password) {
        log.info("RegisterBrokerFromValidation start validationId={} email={}", validationId, email);
        assertEmailNotTaken(email);

        var cached = tempStore.getValidation(validationId);
        Broker broker = new Broker();
        if (cached != null) {
            String mc = cached.mcNumber;
            if (mc == null) {
                mc = tempStore.getBrokerMcNumber(validationId);
                log.info("Cached result had null mcNumber, using entry mcNumber from tempStore: {}", mc);
            }
            broker.setMcNumber(mc);
            broker.setDotNumber(cached.dotNumber);
            broker.setLegalName(cached.legalName);
            broker.setOperatingStatus(cached.operatingStatus);
            broker.setBrokerAuthorityActive(Boolean.TRUE.equals(cached.brokerAuthorityActive));
        } else {
            BrokerValidation bv = brokerValidationRepo.findById(validationId).orElseThrow();
            broker.setMcNumber(bv.getMcNumber());
            broker.setDotNumber(bv.getDotNumber());
            broker.setLegalName(bv.getLegalName());
            broker.setOperatingStatus(bv.getOperatingStatus());
            broker.setBrokerAuthorityActive(bv.isBrokerAuthorityActive());
        }
        if (broker.getMcNumber() == null) {
            log.error("Failed to register broker: mcNumber is null for validationId={}", validationId);
            throw new RuntimeException("Broker mcNumber missing from validation result; cannot persist");
        }

        removeOrphanedBroker(broker.getMcNumber());
        // Guard: ensure this MC/DOT is not already registered as a Carrier
        assertMcAndDotNotUsedByCarrier(broker.getDotNumber(), broker.getMcNumber());
        brokerRepository.save(broker);
        log.info("Broker persisted from validation validationId={} brokerId={}", validationId, broker.getId());

    tempStore.removeValidation(validationId);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.ROLE_BROKER);
        user.setBroker(broker);
    user.setEmailVerified(false);
        user.setAdminApproved(false);

        userRepository.save(user);
        log.info("RegisterBrokerFromValidation success validationId={} email={} userId={} brokerId={}", validationId, user.getEmail(), user.getId(), broker.getId());

    return new LoginResponse(
        jwtUtil.generateToken(user),
        user.getId().toString(),
        user.getEmail(),
        user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
        user.isAdminApproved()
    );
    }

    @Transactional
    public LoginResponse registerBroker(RegisterBrokerRequest req) {
        log.info("RegisterBroker start mcNumber={} email={}", req.mcNumber(), req.email());
        assertEmailNotTaken(req.email());

        BrokerValidationResult validation = brokerValidationService.validate(req.mcNumber());

        Broker broker = new Broker();
        broker.setMcNumber(validation.getMcNumber());
        broker.setDotNumber(validation.getDotNumber());
        broker.setLegalName(validation.getLegalName());
        broker.setOperatingStatus(validation.getOperatingStatus());
        broker.setBrokerAuthorityActive(validation.isBrokerAuthorityActive());

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
        log.info("RegisterBroker success email={} userId={} brokerId={}", user.getEmail(), user.getId(), broker.getId());

        return new LoginResponse(
                jwtUtil.generateToken(user),
                user.getId().toString(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : null,
                user.isAdminApproved()
        );
    }

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
     * Minimal broker registration — creates a User with ROLE_BROKER but no Broker entity yet.
     * The broker profile (MC, DOT, company info) is completed later via the profile wizard.
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

        log.info("RegisterBrokerMinimal success email={} userId={}", email, user.getId());
        return authService.issueTokenForUser(user);
    }

    /**
     * Minimal carrier registration — creates a User with ROLE_CARRIER but no Carrier entity yet.
     * The carrier profile (DOT, MC, company info) is completed later via the profile wizard.
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

        log.info("RegisterCarrierMinimal success email={} userId={}", email, user.getId());
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
