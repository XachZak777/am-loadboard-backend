package am.loadboardbackend.dto.load;

import am.loadboardbackend.model.Address;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LoadResponseDto(
        UUID id,
        List<Address> originAddresses,
        List<Address> destinationAddresses,
        LocalDate pickupDate,
        BigDecimal rate,
        String status
) {}
