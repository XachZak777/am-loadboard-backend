package am.loadboardbackend.controller;

import am.loadboardbackend.dto.CreateLoadRequest;
import am.loadboardbackend.dto.LoadPostingDto;
import am.loadboardbackend.service.LoadPostingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loads")
public class LoadPostingController {

    private final LoadPostingService loadService;

    public LoadPostingController(LoadPostingService loadService) {
        this.loadService = loadService;
    }

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
}
