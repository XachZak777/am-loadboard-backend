package am.loadboardbackend.service;

import am.loadboardbackend.service.validation.ValidationPayload;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class TemporaryValidationStore {

    private final Cache<UUID, ValidationPayload> validationCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .maximumSize(10_000)
        .build();

    // Keep a small broker mcNumber cache for backward compatibility with existing code paths that expect it
    private final Cache<UUID, String> brokerMcCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .maximumSize(10_000)
        .build();

    public UUID storeValidation(ValidationPayload payload) {
        UUID id = UUID.randomUUID();
        return storeValidation(id, payload);
    }

    /**
     * Store validation using a pre-generated id so callers can persist DB records with the same id.
     */
    public UUID storeValidation(UUID id, ValidationPayload payload) {
        validationCache.put(id, payload);
        if (payload.mcNumber != null) {
            brokerMcCache.put(id, payload.mcNumber);
        }
        log.info("Cached validation id={} lookupType={} lookupValue={}", id, payload.lookupType, payload.lookupValue);
        return id;
    }

    public ValidationPayload getValidation(UUID id) {
        return validationCache.getIfPresent(id);
    }

    public void removeValidation(UUID id) { validationCache.invalidate(id); brokerMcCache.invalidate(id); }

    // Backwards-compatible helpers for broker-specific code
    public String getBrokerMcNumber(UUID id) { return brokerMcCache.getIfPresent(id); }

    /**
     * Store an explicit mcNumber into the broker mc cache for a given validation id.
     * Used when the parsed payload.mcNumber is null but we want to retain the lookup MC.
     */
    public void storeBrokerMcNumber(UUID id, String mcNumber) {
        if (mcNumber != null) {
            brokerMcCache.put(id, mcNumber);
        }
    }
}
