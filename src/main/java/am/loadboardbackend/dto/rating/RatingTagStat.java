package am.loadboardbackend.dto.rating;

public record RatingTagStat(
        String tag,
        long count,
        long total
) {}
