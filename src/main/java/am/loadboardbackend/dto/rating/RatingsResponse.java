package am.loadboardbackend.dto.rating;

import java.util.List;

public record RatingsResponse(
        long positiveCount,
        long negativeCount,
        List<RatingTagStat> tagStats,
        List<RatingDto> ratings
) {}
