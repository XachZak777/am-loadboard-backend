package am.loadboardbackend.dto.load;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateBidRequest(
        BigDecimal amount,
        LocalDate requestedPickupDate,
        String requestedPickupTime,
        LocalDate requestedDropDate,
        String requestedDropTime,
        String notes
) {}
