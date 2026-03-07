package am.loadboardbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LoadPostingDto {
    private UUID id;
    private String pickupStreet;
    private String pickupCity;
    private String pickupState;
    private String pickupZip;
    private String pickupCountry;
    private String deliveryStreet;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryZip;
    private String deliveryCountry;
    private String description;
    private Double weight;
    private Double price;
    private LocalDateTime createdAt;
    private UUID carrierId;
    private UUID assignedCarrierId;
    private String status;
}
