package am.loadboardbackend.service;

import am.loadboardbackend.dto.CarrierResponseDto;
import am.loadboardbackend.dto.LoginResponse;
import am.loadboardbackend.dto.RegisterCarrierRequest;
import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.CarrierRepository;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepo;
    private final CarrierRepository carrierRepo;
    private final PasswordEncoder passwordEncoder;
    private final CarrierValidationService carrierValidation;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse registerCarrier(RegisterCarrierRequest req) {

        CarrierResponseDto api = carrierValidation.validate(
                req.lookupValue(),
                req.lookupType()
        );

        Carrier carrier = new Carrier();
        carrier.setDotNumber(api.dotNumber());
        carrier.setMcNumber(api.mcNumber());
        carrier.setLegalName(api.legalName());
        carrier.setDbaName(api.rawData().getDbaName());
        carrier.setOperatingStatus(api.operatingStatus());
        carrier.setSafetyRating(api.safetyRating());
        carrier.setVerified(true);
        carrier.setPhyStreet(api.rawData().getPhyStreet());
        carrier.setPhyCity(api.rawData().getPhyCity());
        carrier.setPhyState(api.rawData().getPhyState());
        carrier.setPhyZip(api.rawData().getPhyZipcode());
        carrier.setPhyCountry(api.rawData().getPhyCountry());
        carrier.setTotalDrivers(api.rawData().getTotalDrivers());
        carrier.setTotalPowerUnits(api.rawData().getTotalPowerUnits());
        carrier.setRawFmcsa("{}"); 

        carrierRepo.save(carrier);

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(User.Role.ROLE_CARRIER);
        user.setCarrier(carrier);

        userRepo.save(user);

        return new LoginResponse(
                jwtUtil.generateToken(user)
        );
    }
}
