package am.loadboardbackend.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder.build();
        restTemplate.setInterceptors(List.of(new FmcsaUserAgentInterceptor()));
        return restTemplate;
    }

    /**
     * The FMCSA mobile API (mobile.fmcsa.dot.gov) blocks requests with non-browser
     * User-Agent strings (e.g. Java/21) and returns 503. This interceptor injects a
     * standard browser UA so the WAF lets the request through.
     */
    private static class FmcsaUserAgentInterceptor implements ClientHttpRequestInterceptor {
        private static final String BROWSER_UA =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36";

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set("User-Agent", BROWSER_UA);
            request.getHeaders().set("Accept", "application/json");
            return execution.execute(request, body);
        }
    }
}
