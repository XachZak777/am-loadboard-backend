package am.loadboardbackend.dto.ai;

import java.util.List;

public record AiSupportRequest(
        String message,
        List<HistoryMessage> history
) {
    public record HistoryMessage(String role, String content) {}
}
