package am.loadboardbackend.dto.document;

public record DocumentUploadResponse(
        String fileId,
        String fileName,
        String fileUrl,
        String uploadedAt
) {}
