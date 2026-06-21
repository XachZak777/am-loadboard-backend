package am.loadboardbackend.dto.rating;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RatingDto(
        UUID id,
        String type,
        String fromName,
        String fromRole,
        String loadTitle,
        List<String> tags,
        String comment,
        LocalDateTime createdAt,
        UUID loadId
) {}
