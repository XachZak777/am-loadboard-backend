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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStorageService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final String BASE_UPLOAD_DIR = "uploads";

    private final DocumentRepository documentRepository;

    /**
     * Store a W9 file for the given owner and persist metadata.
     */
    @Transactional
    public DocumentUploadResponse storeW9(MultipartFile file, UUID ownerId, String ownerType) {
        return storeDocument(file, ownerId, ownerType, "W9", BASE_UPLOAD_DIR + "/w9");
    }

    /**
     * Store an Insurance Certificate file for the given owner and persist metadata.
     */
    @Transactional
    public DocumentUploadResponse storeInsurance(MultipartFile file, UUID ownerId, String ownerType) {
        return storeDocument(file, ownerId, ownerType, "INSURANCE", BASE_UPLOAD_DIR + "/insurance");
    }

    /**
     * Store an MC Authority file for the given owner and persist metadata.
     */
    @Transactional
    public DocumentUploadResponse storeMcAuthority(MultipartFile file, UUID ownerId, String ownerType) {
        return storeDocument(file, ownerId, ownerType, "MC_AUTHORITY", BASE_UPLOAD_DIR + "/mc-authority");
    }

    /**
     * Generic document storage — store a file of any documentType.
     *
     * @param file         the uploaded file
     * @param ownerId      UUID of the carrier or broker record
     * @param ownerType    "CARRIER" or "BROKER"
     * @param documentType e.g. "W9", "INSURANCE", "MC_AUTHORITY"
     * @param uploadDir    relative directory path (e.g. "uploads/w9")
     * @return response DTO with fileId, fileName, fileUrl, uploadedAt
     */
    @Transactional
    public DocumentUploadResponse storeDocument(MultipartFile file, UUID ownerId, String ownerType,
                                                String documentType, String uploadDir) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : documentType.toLowerCase());
        String extension = extractExtension(originalFilename);

        UUID fileId = UUID.randomUUID();
        String storedFileName = fileId + "." + extension;
        Path uploadPath = Paths.get(uploadDir);
        Path destination = uploadPath.resolve(storedFileName);

        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("{} file stored: path={} ownerId={} ownerType={}", documentType, destination, ownerId, ownerType);
        } catch (IOException e) {
            log.error("Failed to store {} file for ownerId={}", documentType, ownerId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        String fileUrl = "/" + uploadDir + "/" + storedFileName;

        Document document = new Document();
        // Do NOT set the ID manually — let @GeneratedValue assign it.
        document.setOwnerId(ownerId);
        document.setOwnerType(ownerType);
        document.setDocumentType(documentType);
        document.setOriginalName(originalFilename);
        document.setStoredPath(destination.toString());
        document.setFileUrl(fileUrl);

        Document saved = documentRepository.save(document);
        log.info("Document metadata persisted id={} type={} ownerId={}", saved.getId(), documentType, ownerId);

        return new DocumentUploadResponse(
                saved.getId().toString(),
                storedFileName,
                fileUrl,
                saved.getUploadedAt().toString()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File size exceeds 5MB limit");
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
}
