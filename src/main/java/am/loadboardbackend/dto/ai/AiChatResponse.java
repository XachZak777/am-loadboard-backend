package am.loadboardbackend.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiChatResponse {

    private boolean success;
    private String conversationId;
    private MessageResponse response;
    private ResponseMetadata metadata;
    private ErrorDetail error;

    @Data
    @Builder
    public static class MessageResponse {
        private String messageId;
        private String content;
        private String timestamp;
        private List<String> relatedLoadIds;
        private double confidence;
    }

    @Data
    @Builder
    public static class ResponseMetadata {
        private long processingTimeMs;
        private int loadsQueried;
        private int loadsMatched;
        private String aiModel;
        private int messagesUsed;
        private int messagesLimit;
        private String resetAt;
    }

    @Data
    @Builder
    public static class ErrorDetail {
        private String code;
        private String message;
        private String details;
        private String timestamp;
        private String resetAt;
    }
}
