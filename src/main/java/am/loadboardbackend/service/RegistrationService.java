package am.loadboardbackend.service;

import am.loadboardbackend.dto.carrier.CarrierResponseDto;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.RegisterCarrierRequest;
import am.loadboardbackend.dto.broker.BrokerResponseDto;
import am.loadboardbackend.dto.broker.RegisterBrokerRequest;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import am.loadboardbackend.repository.BrokerRepository;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.security.JwtUtil;
import am.loadboardbackend.service.validation.BrokerValidationResult;
import am.loadboardbackend.service.validation.CarrierValidationResult;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final CarrierRepository carrierRepository;
    private final BrokerRepository brokerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CarrierValidationService carrierValidationService;
    private final BrokerValidationService brokerValidationService;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse registerCarrier(RegisterCarrierRequest req) {

        CarrierValidationResult validation =
                carrierValidationService.validate(req.lookupValue(), req.lookupType());

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
        carrier.setRawFmcsa(validation.getRawFmcsaJson());

        carrierRepository.save(carrier);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_CARRIER);
        user.setCarrier(carrier);

        userRepository.save(user);

        return new LoginResponse(
                jwtUtil.generateToken(user)
        );
    }

    @Transactional
    public LoginResponse registerBroker(RegisterBrokerRequest req) {

        BrokerValidationResult validation =
                brokerValidationService.validate(req.mcNumber());

        Broker broker = new Broker();
        broker.setMcNumber(validation.getMcNumber());
        broker.setDotNumber(validation.getDotNumber());
        broker.setLegalName(validation.getLegalName());
        broker.setOperatingStatus(validation.getOperatingStatus());
        broker.setBrokerAuthorityActive(true);
        broker.setRawFmcsa(validation.getRawFmcsaJson());

        brokerRepository.save(broker);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(UserRole.ROLE_BROKER);
        user.setBroker(broker);

        userRepository.save(user);

        return new LoginResponse(
                jwtUtil.generateToken(user)
        );
    }

}
