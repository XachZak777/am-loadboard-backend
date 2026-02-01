package am.loadboardbackend.service;

import am.loadboardbackend.client.FmcsaClient;
import am.loadboardbackend.dto.CarrierLookupType;
import am.loadboardbackend.dto.CarrierResponseDto;
import am.loadboardbackend.dto.FmcsaCarrierResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarrierValidationService {
    private final FmcsaClient fmcsaClient;

    public CarrierResponseDto validate(String value, CarrierLookupType type) {

        FmcsaCarrierResponse raw = (type == CarrierLookupType.DOT)
                ? fmcsaClient.fetchByDot(value)
                : fmcsaClient.fetchByMc(value);

        if (raw == null || raw.getContent() == null || raw.getContent().isEmpty()) {
            throw new RuntimeException("Carrier not found in FMCSA");
        }

        var carrier = raw.getContent().get(0).getCarrier();

        return new CarrierResponseDto(
                carrier.getDotNumber(),
                carrier.getMcNumber(),
                carrier.getLegalName(),
                carrier.getStatusCode(),
                carrier.getAllowedToOperate(),
                true,
                carrier
        );
    }
}
