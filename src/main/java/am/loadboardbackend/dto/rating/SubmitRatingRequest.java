package am.loadboardbackend.dto.rating;

import java.util.List;
import java.util.UUID;

public record SubmitRatingRequest(
        UUID targetId,
        String targetType,
        UUID loadId,
        String type,
        List<String> tags,
        String comment
) {}
