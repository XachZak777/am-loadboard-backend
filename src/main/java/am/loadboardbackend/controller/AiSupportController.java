package am.loadboardbackend.controller;

import am.loadboardbackend.dto.ai.AiSupportRequest;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.AiSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/support")
@RequiredArgsConstructor
public class AiSupportController {

    private final AiSupportService aiSupportService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody AiSupportRequest request,
            Authentication authentication
    ) {
        User user = null;
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User u) {
            user = u;
        }
        return ResponseEntity.ok(Map.of("response", aiSupportService.process(request, user)));
    }
}
