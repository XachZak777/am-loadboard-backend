package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_chat_usage")
@Data
public class AiChatUsage {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private int messageCount = 0;

    @Column(nullable = false)
    private LocalDateTime windowStart;
}
