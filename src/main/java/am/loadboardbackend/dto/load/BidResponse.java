package am.loadboardbackend.dto.load;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BidResponse(
        UUID id,
        UUID loadId,
        UUID carrierId,
        BigDecimal amount,
        boolean bookNow,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
