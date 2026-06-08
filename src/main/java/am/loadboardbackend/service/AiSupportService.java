package am.loadboardbackend.service;

import am.loadboardbackend.dto.ai.AiSupportRequest;
import am.loadboardbackend.model.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiSupportService {

    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${anthropic.model:claude-sonnet-4-6}")
    private String anthropicModel;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;

    public String process(AiSupportRequest request, User user) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return "AI support is currently unavailable. Please try again later.";
        }

        List<Map<String, String>> messages = new ArrayList<>();

        // Build history — must start with 'user' and strictly alternate
        if (request.history() != null) {
            for (var h : request.history()) {
                if ("user".equals(h.role()) || "assistant".equals(h.role())) {
                    messages.add(Map.of("role", h.role(), "content", h.content()));
                }
            }
        }

        // Append user context if authenticated, then add the current message
        String userContent = user != null ? appendUserContext(request.message(), user) : request.message();
        messages.add(Map.of("role", "user", "content", userContent));

        Map<String, Object> claudeRequest = Map.of(
                "model", anthropicModel,
                "max_tokens", MAX_TOKENS,
                "system", buildSystemPrompt(),
                "messages", messages
        );

        try {
            String responseBody = RestClient.create()
                    .post()
                    .uri(ANTHROPIC_API_URL)
                    .header("x-api-key", anthropicApiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(claudeRequest)
                    .retrieve()
                    .body(String.class);

            return extractContent(responseBody);
        } catch (Exception e) {
            log.error("AI support error: {}", e.getMessage());
            return "I'm having trouble connecting right now. Please try again in a moment.";
        }
    }

    private String appendUserContext(String message, User user) {
        StringBuilder sb = new StringBuilder(message);
        sb.append("\n\n[User context: role=");
        sb.append(user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : "unknown");
        if (user.getCarrier() != null && user.getCarrier().getCompanyName() != null) {
            sb.append(", company=").append(user.getCarrier().getCompanyName());
        } else if (user.getBroker() != null && user.getBroker().getCompanyName() != null) {
            sb.append(", company=").append(user.getBroker().getCompanyName());
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildSystemPrompt() {
        return """
                You are a helpful customer support assistant for Haulius, a vehicle transportation load board platform that connects brokers with carriers.

                USER ROLES:
                - Broker: Posts vehicle transport loads, manages bookings, approves/rejects carrier bids, pays carriers
                - Carrier: Browses the load board, places bids on open loads, picks up and delivers vehicles, confirms payment receipt

                LOAD WORKFLOW:
                1. Broker posts a load (vehicle details, pickup/delivery locations and dates, price)
                2. Carriers browse open loads and place bids
                3. Broker approves a bid — load becomes ASSIGNED to that carrier
                4. Carrier confirms pickup → PICKED_UP
                5. Carrier delivers the vehicle → DELIVERED
                6. Carrier confirms payment received → PAID / COMPLETED

                KEY FEATURES:
                - Load Board (/loads): Browse and filter open loads
                - Bidding: Carriers bid; brokers approve or reject
                - AI Load Matcher: Amber floating button on the load board — finds loads matching a carrier's route and equipment
                - Dispatch Sheet: Auto-generated document with all shipment details after booking
                - Ratings: Mutual ratings after completed loads, visible on company profiles
                - Documents: W9 and other files uploaded to carrier/broker profiles
                - FMCSA Verification: DOT/MC numbers verified through FMCSA
                - Email Notifications: Automatic emails for bid approvals, status changes, payments
                - Company Profiles: /company/broker/:id or /company/carrier/:id

                NAVIGATION:
                - Load Board: /loads
                - Post a Load (broker): New Load button on /broker/loads
                - My Loads (broker): /broker/loads
                - My Loads (carrier): /carrier/loads
                - Account Settings: /settings

                Answer concisely. For account-specific questions (specific load status, payment details, booking info), direct users to their dashboard.
                """;
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentArray = root.path("content");
            if (contentArray.isArray() && !contentArray.isEmpty()) {
                return contentArray.get(0).path("text").asString("");
            }
        } catch (Exception e) {
            log.error("Failed to parse Claude response: {}", e.getMessage());
        }
        return "I'm sorry, I couldn't process that request. Please try again.";
    }
}
