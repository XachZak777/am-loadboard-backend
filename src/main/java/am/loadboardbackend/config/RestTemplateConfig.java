package am.loadboardbackend.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate used for outbound calls (primarily FMCSA).
     * <ul>
     *   <li>Connect timeout: 10 s — don't hang waiting for TCP handshake</li>
     *   <li>Read timeout:    15 s — FMCSA can be slow but shouldn't exceed this</li>
     *   <li>FmcsaUserAgentInterceptor — spoofs full browser headers so the FMCSA
     *       WAF (which returns 503 for non-browser User-Agents) lets the request through.</li>
     * </ul>
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(45).toMillis());
        return builder
                .requestFactory(() -> factory)
                .additionalInterceptors(new FmcsaUserAgentInterceptor())
                .build();
    }

    /**
     * Injects a full set of browser-like headers on every outbound request.
     *
     * <p>FMCSA's mobile API (mobile.fmcsa.dot.gov) is protected by a WAF that
     * returns 503 SERVICE UNAVAILABLE when it detects non-browser clients.
     * Simply setting a plausible User-Agent is not enough — the WAF also inspects
     * {@code Accept}, {@code Accept-Language}, {@code Accept-Encoding},
     * {@code Referer} and {@code Cache-Control}.  Providing all of them mimics a
     * real Chrome browser request and reliably bypasses the block.</p>
     */
    private static class FmcsaUserAgentInterceptor implements ClientHttpRequestInterceptor {

        private static final String BROWSER_UA =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36";

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set("User-Agent",       BROWSER_UA);
            request.getHeaders().set("Accept",           "application/json, text/plain, */*");
            request.getHeaders().set("Accept-Language",  "en-US,en;q=0.9");
            request.getHeaders().set("Referer",          "https://safer.fmcsa.dot.gov/");
            request.getHeaders().set("Cache-Control",    "no-cache");
            return execution.execute(request, body);
        }
    }
}
