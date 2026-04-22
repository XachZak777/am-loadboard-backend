package am.loadboardbackend.dto.load;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LoadPostingDto {
    private UUID id;

    private String pickupType;
    private String dropType;

    private String pickupStreet;
    private String pickupCity;
    private String pickupState;
    private String pickupZip;
    private String pickupCountry;
    private String pickupLotNumber;

    private String dropStreet;
    private String dropCity;
    private String dropState;
    private String dropZip;
    private String dropCountry;
    private String dropLotNumber;

    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;

    private String description;
    private Double weight;
    private Double price;
    private LocalDate pickupDate;
    private LocalDate deliveryDate;
    private LocalDateTime createdAt;
    private UUID brokerId;
    private UUID carrierId;
    private UUID assignedCarrierId;
    private String status;
}
