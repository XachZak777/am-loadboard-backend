package am.loadboardbackend.controller;

import am.loadboardbackend.dto.vin.VinDecodeResult;
import am.loadboardbackend.service.NhtsaVinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vin")
@RequiredArgsConstructor
public class VinLookupController {

    private final NhtsaVinService nhtsaVinService;

    @GetMapping("/{vin}")
    public ResponseEntity<VinDecodeResult> decode(@PathVariable String vin) {
        VinDecodeResult result = nhtsaVinService.decode(vin);
        return ResponseEntity.ok(result);
    }
}
