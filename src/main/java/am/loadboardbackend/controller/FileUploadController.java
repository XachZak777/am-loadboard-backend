package am.loadboardbackend.controller;

import am.loadboardbackend.model.Document;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.DocumentRepository;
import am.loadboardbackend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;

    /** Upload a W9 PDF — legacy endpoint kept for backward compatibility. */
    @PostMapping(value = "/w9", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> uploadW9(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(fileStorageService.storeW9Pdf(file, user));
    }

    /**
     * Stream any file stored in PostgreSQL by its Document ID.
     * Requires the caller to be authenticated.
     * Returns the file inline with the correct Content-Type header.
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        Document document = documentRepository.findById(UUID.fromString(fileId))
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        ByteArrayResource resource = new ByteArrayResource(document.getFileContent());

        String contentType = document.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + document.getOriginalName() + "\"")
                .body(resource);
    }
}
