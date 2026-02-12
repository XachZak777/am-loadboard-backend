package am.loadboardbackend.mapper;

import am.loadboardbackend.dto.carrier.CarrierPreviewDto;
import am.loadboardbackend.dto.carrier.CarrierResponseDto;
import am.loadboardbackend.model.Carrier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CarrierMapper {

    private final ObjectMapper objectMapper;

    public CarrierResponseDto toResponse(Carrier carrier) {
        return new CarrierResponseDto(
                carrier.getId(),
                carrier.getDotNumber(),
                carrier.getMcNumber(),
                carrier.getLegalName(),
                carrier.getDbaName(),
                carrier.getOperatingStatus(),
                carrier.getSafetyRating(),
                carrier.isVerified(),
                carrier.getPhyCity(),
                carrier.getPhyState(),
                carrier.getTotalDrivers(),
                carrier.getTotalPowerUnits()
        );
    }

    public CarrierPreviewDto toPreview(Carrier carrier) {
        return new CarrierPreviewDto(
                carrier.getDotNumber(),
                carrier.getMcNumber(),
                carrier.getLegalName(),
                carrier.getDbaName(),
                carrier.getPhyCity(),
                carrier.getPhyState()
        );
    }

    public String toRawFmcsaJson(Object fmcsaResponse) {
        try {
            return objectMapper.writeValueAsString(fmcsaResponse);
        } catch (Exception e) {
            throw new IllegalStateException("FMCSA_SERIALIZATION_FAILED", e);
        }
    }
}
