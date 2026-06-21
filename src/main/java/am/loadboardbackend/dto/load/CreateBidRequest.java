package am.loadboardbackend.dto.load;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBidRequest(
        UUID loadId,
        BigDecimal amount,
        boolean bookNow,
        LocalDate requestedPickupDate,
        String requestedPickupTime,
        LocalDate requestedDropDate,
        String requestedDropTime
) {}
