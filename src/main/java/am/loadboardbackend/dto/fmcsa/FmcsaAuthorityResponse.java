package am.loadboardbackend.dto.fmcsa;

import lombok.Data;

@Data
public class FmcsaAuthorityResponse {

    private Authority authority;

    @Data
    public static class Authority {
        private String brokerAuthorityStatus;
    }

    public boolean isBrokerAuthorityActive() {
        return authority != null &&
                "ACTIVE".equalsIgnoreCase(authority.getBrokerAuthorityStatus());
    }
}

