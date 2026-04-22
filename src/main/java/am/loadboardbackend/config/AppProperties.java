package am.loadboardbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Mail mail = new Mail();
    private Frontend frontend = new Frontend();

    @Getter
    @Setter
    public static class Mail {
        private String from;
        private String fromName = "LoadBoard";
    }

    @Getter
    @Setter
    public static class Frontend {
        private String baseUrl = "http://localhost:5173";
    }
}
