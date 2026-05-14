package am.loadboardbackend.controller;

import am.loadboardbackend.dto.load.CreateLoadRequest;
import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.dto.load.CarrierBidWithLoadDto;
import am.loadboardbackend.service.LoadPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loads")
public class LoadPostingController {

    private final LoadPostingService loadService;

    @PostMapping
    public ResponseEntity<LoadPostingDto> createLoad(@Valid @RequestBody CreateLoadRequest req) {
        LoadPostingDto dto = loadService.createLoad(req);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoadPostingDto> updateLoad(@PathVariable UUID id, @Valid @RequestBody CreateLoadRequest req) {
        LoadPostingDto dto = loadService.updateLoad(id, req);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoad(@PathVariable UUID id) {
        loadService.deleteLoad(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/carrier/{carrierId}")
    public ResponseEntity<List<LoadPostingDto>> listForCarrier(@PathVariable UUID carrierId) {
        List<LoadPostingDto> list = loadService.listAllForCarrier(carrierId);
        return ResponseEntity.ok(list);
    }

    @GetMapping
    public ResponseEntity<List<LoadPostingDto>> publicList() {
        List<LoadPostingDto> list = loadService.listAllPublic();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoadPostingDto> getLoad(@PathVariable UUID id) {
        return ResponseEntity.ok(loadService.getLoad(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LoadPostingDto> advanceLoadStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(loadService.advanceLoadStatus(id));
    }

    @GetMapping("/broker/my-loads")
    public ResponseEntity<List<LoadPostingDto>> myBrokerLoads() {
        return ResponseEntity.ok(loadService.listMyBrokerLoads());
    }

    @PostMapping("/bid")
    public ResponseEntity<?> placeBid(@RequestBody am.loadboardbackend.dto.load.CreateBidRequest req) {
        var resp = loadService.placeBid(req);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/bid/{bidId}")
    public ResponseEntity<am.loadboardbackend.dto.load.BidResponse> updateBid(
            @PathVariable java.util.UUID bidId,
            @RequestBody am.loadboardbackend.dto.load.UpdateBidRequest req) {
        return ResponseEntity.ok(loadService.updateBid(bidId, req));
    }

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<am.loadboardbackend.dto.load.BidResponse>> listBids(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(loadService.listBids(id));
    }

    @PostMapping("/{id}/approve/{bidId}")
    public ResponseEntity<Void> approveBid(@PathVariable java.util.UUID id, @PathVariable java.util.UUID bidId) {
        loadService.approveBid(bidId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/auto-assign")
    public ResponseEntity<LoadPostingDto> autoAssignCarrier(@PathVariable UUID id) {
        return ResponseEntity.ok(loadService.autoAssignCarrier(id));
    }

    @PostMapping("/{id}/assign/{carrierId}")
    public ResponseEntity<LoadPostingDto> directAssignCarrier(
            @PathVariable UUID id, @PathVariable UUID carrierId) {
        return ResponseEntity.ok(loadService.directAssignCarrier(id, carrierId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable java.util.UUID id) {
        loadService.cancelBooking(id);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/loads/carrier/my-bids — returns all bids placed by the
     * currently authenticated carrier, each with embedded load details.
     */
    @GetMapping("/carrier/my-bids")
    public ResponseEntity<List<CarrierBidWithLoadDto>> myBids() {
        return ResponseEntity.ok(loadService.getMyCarrierBids());
    }

    @GetMapping("/carrier/preferred-loads")
    public ResponseEntity<List<LoadPostingDto>> preferredLineLoads() {
        return ResponseEntity.ok(loadService.getPreferredLineLoads());
    }
}
