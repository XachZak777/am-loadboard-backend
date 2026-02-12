package am.loadboardbackend.service;

import am.loadboardbackend.client.FmcsaClient;
import am.loadboardbackend.dto.CarrierLookupType;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import am.loadboardbackend.service.validation.CarrierValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class CarrierValidationService {

    private final FmcsaClient fmcsaClient;
    private final ObjectMapper objectMapper;

    public CarrierValidationResult validate(String value, CarrierLookupType type) {

        FmcsaCarrierResponse response =
                (type == CarrierLookupType.DOT)
                        ? fmcsaClient.fetchByDot(value)
                        : fmcsaClient.fetchByMc(value);

        if (response == null ||
                response.getContent() == null ||
                response.getContent().isEmpty()) {
            throw new RuntimeException("Carrier not found in FMCSA");
        }

        try {
            String rawJson = objectMapper.writeValueAsString(response);
            return CarrierValidationResult.from(response, rawJson);
        } catch (Exception e) {
            throw new IllegalStateException("FMCSA_SERIALIZATION_FAILED", e);
        }
    }
}
