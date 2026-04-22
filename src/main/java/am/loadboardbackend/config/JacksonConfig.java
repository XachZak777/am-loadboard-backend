package am.loadboardbackend.config;

/**
 * Jackson is configured via application.properties (spring.jackson.*).
 * See: spring.jackson.deserialization.fail-on-unknown-properties=false
 *      spring.jackson.mapper.accept-case-insensitive-properties=true
 *
 * A separate @Bean ObjectMapper is intentionally avoided to prevent
 * conflicts with Spring Boot's auto-configured jacksonJsonMapper bean.
 */
public final class JacksonConfig {
    private JacksonConfig() {}
}
