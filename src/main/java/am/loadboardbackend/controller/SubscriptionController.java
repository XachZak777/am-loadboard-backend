package am.loadboardbackend.controller;

import am.loadboardbackend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/activate/{carrierId}")
    public ResponseEntity<String> activate(@PathVariable UUID carrierId) {
        String redirectUrl = subscriptionService.activate(carrierId);
        // If stripe session URL returned, return it so frontend can redirect user
        if (redirectUrl != null) return ResponseEntity.ok(redirectUrl);
        return ResponseEntity.ok("activated");
    }

    @PostMapping("/deactivate/{carrierId}")
    public ResponseEntity<String> deactivate(@PathVariable UUID carrierId) {
        subscriptionService.deactivate(carrierId);
        return ResponseEntity.ok("deactivated");
    }
}
