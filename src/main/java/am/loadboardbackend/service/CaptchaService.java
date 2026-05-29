package am.loadboardbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class CaptchaService {

    @Value("${captcha.secret-key}")
    private String secretKey;

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public void verify(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA verification is required");
        }
        RestTemplate rt = new RestTemplate();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", token);
        Map<?, ?> response = rt.postForObject(VERIFY_URL, params, Map.class);
        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA verification failed. Please try again.");
        }
    }
}
