package am.loadboardbackend.service;

import am.loadboardbackend.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    public Map<String, String> storeW9Pdf(MultipartFile file, User user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "w9" : file.getOriginalFilename());
        if (!original.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are supported");
        }

        try {
            Path dir = Path.of("uploads", "w9", user.getId().toString());
            Files.createDirectories(dir);
            Path dst = dir.resolve(original);
            Files.copy(file.getInputStream(), dst, StandardCopyOption.REPLACE_EXISTING);

            return Map.of(
                    "fileName", original,
                    "path", dst.toString()
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
    }
}
