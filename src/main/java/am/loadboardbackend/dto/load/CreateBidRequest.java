package am.loadboardbackend.dto.load;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateBidRequest(
        UUID loadId,
        BigDecimal amount,
        boolean bookNow
) {}
