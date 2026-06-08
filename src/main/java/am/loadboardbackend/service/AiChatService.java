package am.loadboardbackend.service;

import am.loadboardbackend.dto.ai.AiChatRequest;
import am.loadboardbackend.dto.ai.AiChatResponse;
import am.loadboardbackend.dto.ai.AiUsageResponse;
import am.loadboardbackend.dto.load.AdditionalVehicleRequest;
import am.loadboardbackend.model.AiChatUsage;
import am.loadboardbackend.model.LoadPosting;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.AiChatUsageRepository;
import am.loadboardbackend.repository.LoadPostingRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatService {

    private final LoadPostingRepository loadPostingRepository;
    private final AiChatUsageRepository usageRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${anthropic.model:claude-sonnet-4-5}")
    private String anthropicModel;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;
    private static final int MAX_LOADS = 50;
    private static final int MESSAGE_LIMIT = 5;
    private static final int WINDOW_HOURS = 24;

    public AiUsageResponse getUsage(UUID userId) {
        AiChatUsage usage = resolveUsage(userId);
        LocalDateTime resetAt = usage.getWindowStart().plusHours(WINDOW_HOURS);
        return AiUsageResponse.builder()
                .messagesUsed(usage.getMessageCount())
                .messagesLimit(MESSAGE_LIMIT)
                .limitReached(usage.getMessageCount() >= MESSAGE_LIMIT)
                .resetAt(resetAt.toInstant(ZoneOffset.UTC).toString())
                .build();
    }

    @Transactional
    public AiChatResponse processChat(AiChatRequest request) {
        long startTime = System.currentTimeMillis();
        User user = authService.currentUserOrThrow();
        checkAndIncrementUsage(user.getId());

        List<LoadPosting> openLoads = loadPostingRepository.findAll()
                .stream()
                .filter(l -> l.getStatus() == null || l.getStatus() == LoadPosting.LoadStatus.OPEN)
                .limit(MAX_LOADS)
                .collect(Collectors.toList());

        int loadsQueried = openLoads.size();

        String promptContent = buildPrompt(request.getMessage(), user, openLoads);

        Map<String, Object> claudeRequest = Map.of(
                "model", anthropicModel,
                "max_tokens", MAX_TOKENS,
                "system", buildSystemPrompt(),
                "messages", List.of(Map.of("role", "user", "content", promptContent))
        );

        String responseBody = RestClient.create()
                .post()
                .uri(ANTHROPIC_API_URL)
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(claudeRequest)
                .retrieve()
                .body(String.class);

        String content = extractContent(responseBody);
        List<String> relatedLoadIds = extractOrderIds(content, openLoads);
        long processingTime = System.currentTimeMillis() - startTime;

        AiChatUsage usage = resolveUsage(user.getId());
        String resetAt = usage.getWindowStart().plusHours(WINDOW_HOURS).toInstant(ZoneOffset.UTC).toString();

        log.info("AI chat processed: user={}, messagesUsed={}, loadsQueried={}, ms={}",
                user.getId(), usage.getMessageCount(), loadsQueried, processingTime);

        return AiChatResponse.builder()
                .success(true)
                .conversationId(request.getConversationId())
                .response(AiChatResponse.MessageResponse.builder()
                        .messageId("msg_" + System.currentTimeMillis())
                        .content(content)
                        .timestamp(Instant.now().toString())
                        .relatedLoadIds(relatedLoadIds)
                        .confidence(0.9)
                        .build())
                .metadata(AiChatResponse.ResponseMetadata.builder()
                        .processingTimeMs(processingTime)
                        .loadsQueried(loadsQueried)
                        .loadsMatched(relatedLoadIds.size())
                        .aiModel(anthropicModel)
                        .messagesUsed(usage.getMessageCount())
                        .messagesLimit(MESSAGE_LIMIT)
                        .resetAt(resetAt)
                        .build())
                .build();
    }

    private AiChatUsage resolveUsage(UUID userId) {
        AiChatUsage usage = usageRepository.findById(userId).orElseGet(() -> {
            AiChatUsage fresh = new AiChatUsage();
            fresh.setUserId(userId);
            fresh.setMessageCount(0);
            fresh.setWindowStart(LocalDateTime.now());
            return fresh;
        });
        // Reset window if 24 h have passed
        if (usage.getWindowStart().plusHours(WINDOW_HOURS).isBefore(LocalDateTime.now())) {
            usage.setMessageCount(0);
            usage.setWindowStart(LocalDateTime.now());
            usageRepository.save(usage);
        }
        return usage;
    }

    private void checkAndIncrementUsage(UUID userId) {
        AiChatUsage usage = resolveUsage(userId);
        if (usage.getMessageCount() >= MESSAGE_LIMIT) {
            String resetAt = usage.getWindowStart().plusHours(WINDOW_HOURS)
                    .toInstant(ZoneOffset.UTC).toString();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED|" + resetAt);
        }
        usage.setMessageCount(usage.getMessageCount() + 1);
        usageRepository.save(usage);
    }

    private String buildSystemPrompt() {
        return "You are an AI assistant for Haulius, a vehicle transportation load board platform. " +
                "You help carriers find vehicle transport loads that match their route and equipment. " +
                "Analyze the user's query and the provided load data, then recommend the best matching loads. " +
                "Format your response clearly with load Order ID, route (city, state → city, state), " +
                "distance in miles, price, price per mile, vehicle details, and pickup/delivery dates. " +
                "If no loads match the query, say so politely and suggest the closest alternatives. " +
                "Keep responses concise and focused on the top 5 most relevant loads. " +
                "IMPORTANT: Use plain text only. No markdown of any kind: no **, ##, ---, *, >, ` backticks, or | tables. " +
                "Use numbers like '1.' for lists and simple line breaks for spacing.";
    }

    private String buildPrompt(String userMessage, User user, List<LoadPosting> loads) {
        StringBuilder sb = new StringBuilder();
        sb.append("User Query: ").append(userMessage).append("\n\n");

        sb.append("User Profile:\n");
        sb.append("- Role: ").append(user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : "Unknown").append("\n");
        if (user.getCarrier() != null) {
            var c = user.getCarrier();
            if (c.getCompanyName() != null) sb.append("- Company: ").append(c.getCompanyName()).append("\n");
            if (c.getDotNumber() != null) sb.append("- DOT: ").append(c.getDotNumber()).append("\n");
            if (c.getMcNumber() != null) sb.append("- MC: ").append(c.getMcNumber()).append("\n");
        } else if (user.getBroker() != null) {
            var b = user.getBroker();
            if (b.getCompanyName() != null) sb.append("- Company: ").append(b.getCompanyName()).append("\n");
        } else if (user.getDealer() != null) {
            var d = user.getDealer();
            if (d.getCompanyName() != null) sb.append("- Company: ").append(d.getCompanyName()).append("\n");
        }
        sb.append("\n");

        sb.append("Available Open Loads (").append(loads.size()).append(" total):\n[\n");
        for (int i = 0; i < loads.size(); i++) {
            sb.append(serializeLoad(loads.get(i)));
            if (i < loads.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n\nPlease recommend the best matching loads for this user based on their query.");

        return sb.toString();
    }

    private String serializeLoad(LoadPosting load) {
        StringBuilder sb = new StringBuilder("  {\n");
        sb.append("    \"id\": \"").append(load.getId()).append("\"");

        if (load.getOrderId() != null)
            sb.append(",\n    \"orderId\": \"").append(load.getOrderId()).append("\"");

        if (load.getPickupAddress() != null) {
            if (load.getPickupAddress().getCity() != null)
                sb.append(",\n    \"pickupCity\": \"").append(load.getPickupAddress().getCity()).append("\"");
            if (load.getPickupAddress().getState() != null)
                sb.append(",\n    \"pickupState\": \"").append(load.getPickupAddress().getState()).append("\"");
        }

        if (load.getDropAddress() != null) {
            if (load.getDropAddress().getCity() != null)
                sb.append(",\n    \"deliveryCity\": \"").append(load.getDropAddress().getCity()).append("\"");
            if (load.getDropAddress().getState() != null)
                sb.append(",\n    \"deliveryState\": \"").append(load.getDropAddress().getState()).append("\"");
        }

        if (load.getDistance() != null)
            sb.append(",\n    \"distanceMiles\": ").append(load.getDistance().intValue());

        if (load.getPrice() != null) {
            sb.append(",\n    \"price\": ").append(load.getPrice());
            if (load.getDistance() != null && load.getDistance() > 0)
                sb.append(",\n    \"pricePerMile\": ").append(String.format("%.2f", load.getPrice() / load.getDistance()));
        }

        if (load.getVehicle() != null) {
            var v = load.getVehicle();
            sb.append(",\n    \"vehicle\": {\"year\": ").append(v.getYear())
              .append(", \"make\": \"").append(safe(v.getMake()))
              .append("\", \"model\": \"").append(safe(v.getModel()))
              .append("\", \"type\": \"").append(safe(v.getVehicleType()))
              .append("\", \"condition\": \"").append(safe(v.getCondition())).append("\"}");
            if (v.getTrailerType() != null)
                sb.append(",\n    \"trailerType\": \"").append(v.getTrailerType()).append("\"");
        }

        // Additional vehicles
        if (load.getAdditionalVehicles() != null && !load.getAdditionalVehicles().isBlank()) {
            try {
                List<AdditionalVehicleRequest> extras = objectMapper.readValue(
                        load.getAdditionalVehicles(),
                        new TypeReference<List<AdditionalVehicleRequest>>() {});
                if (!extras.isEmpty()) {
                    sb.append(",\n    \"additionalVehicles\": [");
                    for (int i = 0; i < extras.size(); i++) {
                        var av = extras.get(i);
                        sb.append("{\"year\": ").append(av.vehicleYear())
                          .append(", \"make\": \"").append(safe(av.vehicleMake()))
                          .append("\", \"model\": \"").append(safe(av.vehicleModel()))
                          .append("\", \"type\": \"").append(safe(av.vehicleType())).append("\"}");
                        if (i < extras.size() - 1) sb.append(", ");
                    }
                    sb.append("]");
                }
            } catch (Exception e) {
                log.debug("Could not parse additionalVehicles for load {}", load.getId());
            }
        }

        if (load.getPickupDate() != null)
            sb.append(",\n    \"pickupDate\": \"").append(load.getPickupDate()).append("\"");
        if (load.getDeliveryDate() != null)
            sb.append(",\n    \"deliveryDate\": \"").append(load.getDeliveryDate()).append("\"");

        sb.append("\n  }");
        return sb.toString();
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentArray = root.path("content");
            if (contentArray.isArray() && !contentArray.isEmpty()) {
                return contentArray.get(0).path("text").asString("");
            }
        } catch (Exception e) {
            log.error("Failed to parse Claude API response: {}", e.getMessage());
        }
        return "I'm sorry, I encountered an error processing your request. Please try again in a moment.";
    }

    // Match order IDs mentioned in the response against actual load orderIds
    private List<String> extractOrderIds(String content, List<LoadPosting> loads) {
        List<String> matched = new ArrayList<>();
        for (LoadPosting load : loads) {
            if (load.getOrderId() != null && content.contains(load.getOrderId())) {
                matched.add(load.getOrderId());
            } else if (content.contains(load.getId().toString())) {
                matched.add(load.getId().toString());
            }
        }
        // Fallback: extract alphanumeric tokens that look like IDs
        if (matched.isEmpty()) {
            Pattern pattern = Pattern.compile("\\b([A-Z0-9]{4,20})\\b");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String candidate = matcher.group(1);
                if (!candidate.equals("AI") && !candidate.equals("ID") && !candidate.equals("TX") && !candidate.equals("CA")) {
                    matched.add(candidate);
                }
            }
        }
        return matched.stream().distinct().limit(10).collect(Collectors.toList());
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
