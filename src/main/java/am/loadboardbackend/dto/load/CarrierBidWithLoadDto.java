package am.loadboardbackend.dto.load;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Returned to the authenticated carrier when they request their own bid history.
 * Embeds enough load info so the frontend doesn't need a second request.
 */
public record CarrierBidWithLoadDto(
        UUID bidId,
        UUID loadId,
        BigDecimal amount,
        boolean bookNow,
        String bidStatus,
        LocalDateTime bidCreatedAt,
        LocalDateTime bidUpdatedAt,

        // bid time preferences
        LocalDate requestedPickupDate,
        String requestedPickupTime,
        LocalDate requestedDropDate,
        String requestedDropTime,

        // load info
        String vehicleMake,
        String vehicleModel,
        Integer vehicleYear,
        String pickupCity,
        String pickupState,
        String dropCity,
        String dropState,
        Double price,
        LocalDateTime loadCreatedAt,
        LocalDate pickupDate,
        LocalDate deliveryDate,
        String loadStatus,
        UUID brokerId
) {}
