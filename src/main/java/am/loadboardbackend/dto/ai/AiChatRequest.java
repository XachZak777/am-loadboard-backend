package am.loadboardbackend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 1000, message = "Message too long (max 1000 characters)")
    private String message;

    private String conversationId;

    private Metadata metadata;

    @Data
    public static class Metadata {
        private String timestamp;
        private String userRole;
        private String equipmentType;
    }
}
