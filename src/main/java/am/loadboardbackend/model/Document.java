package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
public class Document {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /**
     * Either 'CARRIER' or 'BROKER'.
     */
    @Column(name = "owner_type", nullable = false, length = 20)
    private String ownerType;

    /**
     * Document type, e.g. 'W9'.
     */
    @Column(name = "document_type", nullable = false, length = 50,
            columnDefinition = "VARCHAR(50) NOT NULL DEFAULT 'W9'")
    private String documentType = "W9";

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "uploaded_at", nullable = false,
            columnDefinition = "TIMESTAMP NOT NULL DEFAULT now()")
    private LocalDateTime uploadedAt;

    @PrePersist
    void onPersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
