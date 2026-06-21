package am.loadboardbackend.dto.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiUsageResponse {
    private int messagesUsed;
    private int messagesLimit;
    private boolean limitReached;
    private String resetAt;
}
