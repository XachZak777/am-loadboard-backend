package am.loadboardbackend.dto.load;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private String pickupContactName;
    private String pickupContactPhone;

    private String dropStreet;
    private String dropCity;
    private String dropState;
    private String dropZip;
    private String dropCountry;
    private String dropLotNumber;
    private String dropContactName;
    private String dropContactPhone;

    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleType;
    private String vehicleCondition;
    private String vin;
    private String trailerType;

    private String description;
    private Double weight;
    private Double price;
    private Double distance;
    private LocalDate pickupDate;
    private String pickupTime;
    private LocalDate deliveryDate;
    private String deliveryTime;
    private LocalDateTime createdAt;
    private UUID brokerId;
    private UUID carrierId;
    private UUID assignedCarrierId;
    private String status;

    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String orderId;
    private String paymentMethod;
    private String paymentTiming;

    private List<AdditionalVehicleRequest> additionalVehicles;
}
