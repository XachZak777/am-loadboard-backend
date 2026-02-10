package am.loadboardbackend.dto.load;

import am.loadboardbackend.model.Address;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateLoadRequest(
        List<Address> originAddresses,
        List<Address> destinationAddresses,
        LocalDate pickupDate,
        BigDecimal rate
) {}

