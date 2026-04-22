package am.loadboardbackend.controller;

import am.loadboardbackend.dto.load.CreateLoadRequest;
import am.loadboardbackend.dto.load.LoadPostingDto;
import am.loadboardbackend.dto.load.CarrierBidWithLoadDto;
import am.loadboardbackend.service.LoadPostingService;
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
    public ResponseEntity<LoadPostingDto> createLoad(@RequestBody CreateLoadRequest req) {
        LoadPostingDto dto = loadService.createLoad(req);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoadPostingDto> updateLoad(@PathVariable UUID id, @RequestBody CreateLoadRequest req) {
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

    @PostMapping("/bid")
    public ResponseEntity<?> placeBid(@RequestBody am.loadboardbackend.dto.load.CreateBidRequest req) {
        var resp = loadService.placeBid(req);
        return ResponseEntity.ok(resp);
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
}
