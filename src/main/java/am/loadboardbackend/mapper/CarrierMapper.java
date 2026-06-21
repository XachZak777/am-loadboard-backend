package am.loadboardbackend.mapper;

import am.loadboardbackend.dto.carrier.CarrierPreviewDto;
import am.loadboardbackend.dto.carrier.CarrierResponseDto;
import am.loadboardbackend.model.Carrier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
 

@Component
@RequiredArgsConstructor
public class CarrierMapper {

    // ObjectMapper removed as raw JSON is no longer produced here

    public CarrierResponseDto toResponse(Carrier carrier) {
        return new CarrierResponseDto(
                carrier.getId(),
                carrier.getDotNumber(),
                carrier.getMcNumber(),
                carrier.getLegalName(),
                carrier.getDbaName(),
                carrier.getCompanyName(),
                carrier.getOperatingStatus(),
                carrier.getSafetyRating(),
                carrier.isVerified(),
                carrier.getPhyCity(),
                carrier.getPhyState(),
                carrier.getTotalDrivers(),
                carrier.getTotalPowerUnits(),
                carrier.getPhoneNumber(),
                carrier.getMailingAddress(),
                carrier.getCity(),
                carrier.getState(),
                carrier.getZipCode(),
                carrier.getInsuranceCompany(),
                carrier.getCargoInsurance(),
                carrier.getLiabilityInsurance(),
                carrier.getTaxIdType(),
                carrier.getTaxId(),
                carrier.getPreferredLines()
        );
    }
}
