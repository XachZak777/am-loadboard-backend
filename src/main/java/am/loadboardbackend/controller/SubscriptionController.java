package am.loadboardbackend.controller;

import am.loadboardbackend.model.Carrier;
import am.loadboardbackend.repository.CarrierRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final CarrierRepository carrierRepo;

    public SubscriptionController(CarrierRepository carrierRepo) {
        this.carrierRepo = carrierRepo;
    }

    @PostMapping("/activate/{carrierId}")
    public ResponseEntity<String> activate(@PathVariable UUID carrierId) {
        Carrier carrier = carrierRepo.findById(carrierId).orElseThrow();
        carrier.setSubscriptionActive(Boolean.TRUE);
        carrierRepo.save(carrier);
        return ResponseEntity.ok("activated");
    }

    @PostMapping("/deactivate/{carrierId}")
    public ResponseEntity<String> deactivate(@PathVariable UUID carrierId) {
        Carrier carrier = carrierRepo.findById(carrierId).orElseThrow();
        carrier.setSubscriptionActive(Boolean.FALSE);
        carrierRepo.save(carrier);
        return ResponseEntity.ok("deactivated");
    }
}
