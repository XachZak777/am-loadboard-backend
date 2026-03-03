package am.loadboardbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LoadPostingDto {
    private UUID id;
    private String pickupCity;
    private String pickupState;
    private String deliveryCity;
    private String deliveryState;
    private String description;
    private Double weight;
    private Double price;
    private LocalDateTime createdAt;
    private UUID carrierId;
}
