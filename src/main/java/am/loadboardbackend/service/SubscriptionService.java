package am.loadboardbackend.service;

import java.util.UUID;

public interface SubscriptionService {
    // Activate subscription for carrier. Returns optional checkout/redirect URL when an external payment provider is configured.
    String activate(UUID carrierId);

    // Deactivate subscription (cancel in DB)
    void deactivate(UUID carrierId);
}
