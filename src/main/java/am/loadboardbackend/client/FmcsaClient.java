package am.loadboardbackend.client;

import am.loadboardbackend.dto.fmcsa.FmcsaAuthorityResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class FmcsaClient {

    @Value("${fmcsa.base-url}")
    private String baseUrl;

    @Value("${fmcsa.api-key}")
    private String webKey;

    private final RestTemplate restTemplate;

    public FmcsaCarrierResponse fetchByDot(String dot) {
        String url = baseUrl + "/carriers/" + dot + "?webKey=" + webKey;
        try {
            return restTemplate.getForObject(url, FmcsaCarrierResponse.class);
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "FMCSA service unavailable", e);
        }
    }

    public FmcsaCarrierResponse fetchByMc(String mc) {
        // corrected path: use 'mc-number' (assumption) — verify with real FMCSA API
        String url = baseUrl + "/carriers/docket-number/" + mc + "?webKey=" + webKey;
        try {
            return restTemplate.getForObject(url, FmcsaCarrierResponse.class);
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "FMCSA service unavailable", e);
        }
    }

    public FmcsaAuthorityResponse fetchAuthority(String dot) {
        String url = baseUrl + "/carriers/" + dot + "/authority?webKey=" + webKey;
        try {
            return restTemplate.getForObject(url, FmcsaAuthorityResponse.class);
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "FMCSA service unavailable", e);
        }
    }

    @PostConstruct
    public void check() {
        System.out.println("FMCSA key loaded: " + (webKey != null));
    }
}

