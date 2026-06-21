package am.loadboardbackend.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single uploaded document as seen by the admin.
 */
public record AdminDocumentDto(
        UUID          documentId,
        String        documentType,
        String        originalName,
        String        fileUrl,
        LocalDateTime uploadedAt
) {}
