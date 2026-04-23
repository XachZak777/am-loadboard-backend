package am.loadboardbackend.service;

import am.loadboardbackend.dto.document.DocumentUploadResponse;
import am.loadboardbackend.model.Document;
import am.loadboardbackend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

/**
 * Stores carrier/broker compliance documents (W9, Insurance, MC Authority) in PostgreSQL.
 * File content is stored as BYTEA; metadata (type, owner, download URL) is persisted in PostgreSQL.
 *
 * <p>Download URL pattern: {@code /api/files/{documentId}}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStorageService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final DocumentRepository documentRepository;
    private final DocumentFileService documentFileService;

    // ── Public typed helpers ──────────────────────────────────────────────────

    @Transactional
    public DocumentUploadResponse storeW9(MultipartFile file, UUID ownerId, String ownerType) {
        return storeDocument(file, ownerId, ownerType, "W9", "w9");
    }

    @Transactional
    public DocumentUploadResponse storeInsurance(MultipartFile file, UUID ownerId, String ownerType) {
        return storeDocument(file, ownerId, ownerType, "INSURANCE", "insurance");
    }

    @Transactional
    public DocumentUploadResponse storeMcAuthority(MultipartFile file, UUID ownerId, String ownerType) {
        return storeDocument(file, ownerId, ownerType, "MC_AUTHORITY", "mc-authority");
    }

    /**
     * Generic document storage — validates, stores file in PostgreSQL BYTEA and persists metadata.
     *
     * @param file         the uploaded file
     * @param ownerId      UUID of the carrier or broker record
     * @param ownerType    "CARRIER" or "BROKER"
     * @param documentType e.g. "W9", "INSURANCE", "MC_AUTHORITY"
     * @param folder       folder tag for organization (unused in PostgreSQL but kept for API compatibility)
     * @return response DTO with fileId, fileName, fileUrl, uploadedAt
     */
    @Transactional
    public DocumentUploadResponse storeDocument(MultipartFile file, UUID ownerId, String ownerType,
                                                String documentType, String folder) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : documentType.toLowerCase());
        String extension = extractExtension(originalFilename);
        String contentType = resolveContentType(extension);

        // Read file content into memory
        byte[] fileContent = documentFileService.readFileContent(file);

        // Persist document with file content to PostgreSQL
        Document document = new Document();
        document.setOwnerId(ownerId);
        document.setOwnerType(ownerType);
        document.setDocumentType(documentType);
        document.setOriginalName(originalFilename);
        document.setFileContent(fileContent);
        document.setContentType(contentType);
    document.setFileUrl("/api/files/temp"); // temp URL, will be updated after save
    // stored_path is a NOT NULL DB column (kept for compatibility). Set a temporary
    // value so the initial insert doesn't violate the constraint; we'll update
    // it to the final path after obtaining the generated ID.
    document.setStoredPath("/api/files/temp");

        Document saved = documentRepository.save(document);
        
    // Update fileUrl and stored_path with document ID
        String fileUrl = "/api/files/" + saved.getId();
        saved.setFileUrl(fileUrl);
    saved.setStoredPath(fileUrl);
        documentRepository.save(saved);
        
        log.info("Document stored in PostgreSQL id={} type={} ownerId={} size={}",
                saved.getId(), documentType, ownerId, fileContent.length);

        return new DocumentUploadResponse(
                saved.getId().toString(),
                originalFilename,
                fileUrl,
                saved.getUploadedAt().toString()
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 5 MB limit");
        }
        String ext = extractExtension(StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : ""));
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PDF, DOC, or DOCX files are allowed");
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1)
                ? filename.substring(dot + 1).toLowerCase()
                : "";
    }

    private String resolveContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf"  -> "application/pdf";
            case "doc"  -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default     -> "application/octet-stream";
        };
    }
}