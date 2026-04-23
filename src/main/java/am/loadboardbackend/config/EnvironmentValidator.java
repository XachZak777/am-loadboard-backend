package am.loadboardbackend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class EnvironmentValidator {

    @PostConstruct
    public void validate() {
        List<String> missing = new ArrayList<>();

        checkRequired("DATASOURCE_URL", missing);
        checkRequired("DATASOURCE_USERNAME", missing);
        checkRequired("DATASOURCE_PASSWORD", missing);
        checkRequired("JWT_SECRET", missing);
        checkRequired("JWT_EXPIRATION_MS", missing);
        checkRequired("MAIL_USERNAME", missing);
        checkRequired("MAIL_PASSWORD", missing);

        checkJwtSecretStrength();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required environment variables: " + String.join(", ", missing)
            );
        }
    }

    private void checkRequired(String name, List<String> missing) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            missing.add(name);
        }
    }

    private void checkJwtSecretStrength() {
        String secret = System.getenv("JWT_SECRET");
        if (secret != null && secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes for HS256 security."
            );
        }
    }
}
