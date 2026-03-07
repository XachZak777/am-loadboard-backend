package am.loadboardbackend.service;

import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.repository.CarrierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final CarrierRepository carrierRepo;

    @Value("${stripe.checkout.url:}")
    private String stripeCheckoutUrl;

    @Override
    public String activate(UUID carrierId) {
        Carrier c = carrierRepo.findById(carrierId).orElseThrow();

        // If no checkout URL configured, just toggle subscription flag (dev mode)
        if (stripeCheckoutUrl == null || stripeCheckoutUrl.isBlank()) {
            c.setSubscriptionActive(Boolean.TRUE);
            carrierRepo.save(c);
            return null;
        }

        // Return a formatted checkout URL for the frontend. The frontend must complete payment and notify backend.
        // The application can interpolate a carrierId parameter so frontend can include it in the return flow.
        return stripeCheckoutUrl + "?carrierId=" + carrierId;
    }

    @Override
    public void deactivate(UUID carrierId) {
        Carrier c = carrierRepo.findById(carrierId).orElseThrow();
        c.setSubscriptionActive(Boolean.FALSE);
        carrierRepo.save(c);
    }
}

