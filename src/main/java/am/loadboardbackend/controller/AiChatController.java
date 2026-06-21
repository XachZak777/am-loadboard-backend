package am.loadboardbackend.controller;

import am.loadboardbackend.dto.ai.AiChatRequest;
import am.loadboardbackend.dto.ai.AiChatResponse;
import am.loadboardbackend.dto.ai.AiUsageResponse;
import am.loadboardbackend.model.User;
import am.loadboardbackend.service.AiChatService;
import am.loadboardbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AiChatService aiChatService;
    private final AuthService authService;

    @GetMapping("/usage")
    public ResponseEntity<AiUsageResponse> getUsage() {
        User user = authService.currentUserOrThrow();
        return ResponseEntity.ok(aiChatService.getUsage(user.getId()));
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        try {
            return ResponseEntity.ok(aiChatService.processChat(request));
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                String resetAt = e.getReason() != null && e.getReason().contains("|")
                        ? e.getReason().split("\\|", 2)[1]
                        : Instant.now().plusSeconds(86400).toString();
                return ResponseEntity.status(429).body(
                        AiChatResponse.builder()
                                .success(false)
                                .conversationId(request.getConversationId())
                                .error(AiChatResponse.ErrorDetail.builder()
                                        .code("RATE_LIMIT_EXCEEDED")
                                        .message("You've used all 3 of your free daily AI messages.")
                                        .resetAt(resetAt)
                                        .timestamp(Instant.now().toString())
                                        .build())
                                .build()
                );
            }
            log.error("AI chat error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse(request, e.getMessage()));
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(errorResponse(request, e.getMessage()));
        }
    }

    private AiChatResponse errorResponse(AiChatRequest request, String details) {
        return AiChatResponse.builder()
                .success(false)
                .conversationId(request.getConversationId())
                .error(AiChatResponse.ErrorDetail.builder()
                        .code("AI_SERVICE_ERROR")
                        .message("Failed to process AI request")
                        .details(details)
                        .timestamp(Instant.now().toString())
                        .build())
                .build();
    }
}
