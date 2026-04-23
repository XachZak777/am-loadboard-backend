package am.loadboardbackend.service;

import am.loadboardbackend.model.Document;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Stores user-level W9 files (legacy {@code /api/files/w9} endpoint) in PostgreSQL.
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final DocumentFileService documentFileService;
    private final DocumentRepository documentRepository;

    @Transactional
    public Map<String, String> storeW9Pdf(MultipartFile file, User user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "w9.pdf" : file.getOriginalFilename());
        if (!original.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are supported");
        }

        // Read file content
        byte[] fileContent = documentFileService.readFileContent(file);

        // Store in PostgreSQL
        Document document = new Document();
        document.setOwnerId(user.getId());
        document.setOwnerType("USER");
        document.setDocumentType("W9");
        document.setOriginalName(original);
        document.setFileContent(fileContent);
        document.setContentType("application/pdf");
        document.setFileUrl("/api/files/temp"); // temp URL, will be updated after save
    document.setStoredPath("/api/files/temp");

        Document saved = documentRepository.save(document);
        
        // Update fileUrl and stored_path with document ID
        String fileUrl = "/api/files/" + saved.getId();
        saved.setFileUrl(fileUrl);
    saved.setStoredPath(fileUrl);
        documentRepository.save(saved);

        return Map.of(
                "fileName", original,
                "fileUrl",  fileUrl,
                "fileId",   saved.getId().toString()
        );
    }
}
