package am.loadboardbackend.service;

import am.loadboardbackend.client.FmcsaClient;
import am.loadboardbackend.dto.fmcsa.FmcsaAuthorityResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import am.loadboardbackend.service.validation.BrokerValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BrokerValidationService {

    private final FmcsaClient fmcsaClient;
    private final ObjectMapper objectMapper;

    public BrokerValidationResult validate(String mcNumber) {

        FmcsaCarrierResponse carrierResponse =
                fmcsaClient.fetchByMc(mcNumber);

        if (carrierResponse == null ||
                carrierResponse.getContent() == null ||
                carrierResponse.getContent().isEmpty()) {
            throw new RuntimeException("Broker not found in FMCSA");
        }

        String dotNumber =
                carrierResponse.getContent()
                        .get(0)
                        .getCarrier()
                        .getDotNumber();

        FmcsaAuthorityResponse authorityResponse =
                fmcsaClient.fetchAuthority(dotNumber);

        if (authorityResponse == null ||
                !authorityResponse.isBrokerAuthorityActive()) {
            throw new RuntimeException("Broker authority is not ACTIVE");
        }

        try {
            String rawJson = objectMapper.writeValueAsString(
                    Map.of(
                            "carrier", carrierResponse,
                            "authority", authorityResponse
                    )
            );

            return BrokerValidationResult.from(
                    carrierResponse,
                    authorityResponse,
                    rawJson
            );

        } catch (Exception e) {
            throw new IllegalStateException("FMCSA_SERIALIZATION_FAILED", e);
        }
    }
}
