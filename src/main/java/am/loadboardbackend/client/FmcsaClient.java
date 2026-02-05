package am.loadboardbackend.client;

import am.loadboardbackend.dto.FmcsaCarrierResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
        return restTemplate.getForObject(url, FmcsaCarrierResponse.class);
    }

    public FmcsaCarrierResponse fetchByMc(String mc) {
        String url = baseUrl + "/carriers/mc-number/" + mc + "?webKey=" + webKey;
        return restTemplate.getForObject(url, FmcsaCarrierResponse.class);
    }

    @PostConstruct
    public void check() {
        System.out.println("FMCSA key loaded: " + (webKey != null));
    }
}

