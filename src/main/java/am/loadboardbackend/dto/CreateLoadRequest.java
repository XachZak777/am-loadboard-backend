package am.loadboardbackend.dto;

import lombok.Data;

@Data
public class CreateLoadRequest {
    private String pickupCity;
    private String pickupState;
    private String deliveryCity;
    private String deliveryState;
    private String description;
    private Double weight;
    private Double price;
}
