package am.loadboardbackend.client;

import am.loadboardbackend.dto.fmcsa.FmcsaAuthorityResponse;
import am.loadboardbackend.dto.fmcsa.FmcsaCarrierResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FmcsaClient {

    private static final int    MAX_ATTEMPTS    = 3;
    private static final long   INITIAL_BACKOFF = 800L;  // ms

    @Value("${fmcsa.base-url}")
    private String baseUrl;

    @Value("${fmcsa.api-key}")
    private String webKey;

    private final RestTemplate restTemplate;

    // ── Public API ────────────────────────────────────────────────────────────

    public FmcsaCarrierResponse fetchByDot(String dot) {
        String url = baseUrl + "/carriers/" + dot + "?webKey=" + webKey;
        log.info("FMCSA fetchByDot → {}", sanitizeUrl(url));
        return executeWithRetry(url, FmcsaCarrierResponse.class, "DOT=" + dot);
    }

    public FmcsaCarrierResponse fetchByMc(String mc) {
        String url = baseUrl + "/carriers/docket-number/" + mc + "?webKey=" + webKey;
        log.info("FMCSA fetchByMc → {}", sanitizeUrl(url));
        return executeWithRetry(url, FmcsaCarrierResponse.class, "MC=" + mc);
    }

    public FmcsaAuthorityResponse fetchAuthority(String dot) {
        String url = baseUrl + "/carriers/" + dot + "/authority?webKey=" + webKey;
        log.info("FMCSA fetchAuthority → {}", sanitizeUrl(url));
        return executeWithRetry(url, FmcsaAuthorityResponse.class, "authority DOT=" + dot);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /**
     * Calls the FMCSA REST endpoint up to {@value MAX_ATTEMPTS} times with
     * exponential back-off.  Only 503 / connection-level errors are retried;
     * 4xx errors (bad key, not found, …) are surfaced immediately.
     */
    private <T> T executeWithRetry(String url, Class<T> responseType, String context) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                T result = restTemplate.getForObject(url, responseType);
                if (result instanceof FmcsaCarrierResponse r && r.getContent() == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Carrier not found in FMCSA for " + context);
                }
                if (attempt > 1) {
                    log.info("FMCSA call succeeded on attempt {} for {}", attempt, context);
                }
                return result;

            } catch (HttpClientErrorException ex) {
                // 4xx — don't retry, surface the real status
                HttpStatus status = (HttpStatus) ex.getStatusCode();
                log.error("FMCSA client error {} for {}: {}", status, context, ex.getResponseBodyAsString());
                if (status == HttpStatus.NOT_FOUND) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Carrier not found in FMCSA (404)", ex);
                }
                if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "FMCSA API key invalid or forbidden (" + status + ")", ex);
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "FMCSA returned " + status + " for " + context, ex);

            } catch (HttpServerErrorException ex) {
                // 5xx (503, 500, …) — log and retry
                log.warn("FMCSA server error {} on attempt {}/{} for {}: {}",
                        ex.getStatusCode(), attempt, MAX_ATTEMPTS, context,
                        ex.getResponseBodyAsString());
                lastException = ex;

            } catch (ResourceAccessException ex) {
                // Network / timeout — log and retry
                log.warn("FMCSA network error on attempt {}/{} for {}: {}",
                        attempt, MAX_ATTEMPTS, context, ex.getMessage());
                lastException = ex;
            }

            if (attempt < MAX_ATTEMPTS) {
                long backoff = INITIAL_BACKOFF * (1L << (attempt - 1)); // 800 ms, 1600 ms
                log.info("Retrying FMCSA call in {} ms (attempt {}/{})", backoff, attempt + 1, MAX_ATTEMPTS);
                try { Thread.sleep(backoff); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("All {} FMCSA attempts failed for {}", MAX_ATTEMPTS, context);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "FMCSA service unavailable after " + MAX_ATTEMPTS + " attempts", lastException);
    }

    /** Strip the webKey from URLs before writing to logs. */
    private static String sanitizeUrl(String url) {
        return url.replaceAll("([?&]webKey=)[^&]+", "$1***");
    }

    @PostConstruct
    public void check() {
        boolean keyPresent = webKey != null && !webKey.isBlank();
        log.info("FMCSA client initialized — baseUrl={} apiKeyPresent={}",
                baseUrl, keyPresent);
        if (!keyPresent) {
            log.warn("FMCSA API key is blank — all carrier lookups will fail with 403/503");
        }
    }
}


