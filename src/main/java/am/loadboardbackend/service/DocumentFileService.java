package am.loadboardbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Stores and retrieves file content directly from PostgreSQL BYTEA columns.
 * Replaces MongoDB GridFS for document storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentFileService {

    /**
     * Read a file's binary content from a MultipartFile.
     *
     * @param file the multipart file to read
     * @return byte array containing the file content
     */
    public byte[] readFileContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        try {
            byte[] content = file.getBytes();
            log.info("File content read successfully: {} bytes", content.length);
            return content;
        } catch (IOException e) {
            log.error("Failed to read file input stream", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file");
        } catch (Exception e) {
            log.error("Error processing file", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process file");
        }
    }
}
