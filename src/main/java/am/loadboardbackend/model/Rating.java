package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ratings")
@Data
public class Rating {

    @Id
    @GeneratedValue
    private UUID id;

    /** Profile ID of the rated broker or carrier */
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType; // "broker" | "carrier"

    @Column(name = "load_id", nullable = false)
    private UUID loadId;

    @Column(name = "submitter_id", nullable = false)
    private UUID submitterId;

    @Column(nullable = false, length = 20)
    private String type; // "positive" | "negative"

    /** JSON array of tag strings, e.g. ["communication","payment"] */
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
