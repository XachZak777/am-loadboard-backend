package am.loadboardbackend.dto.load;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateLoadRequest {

    @NotBlank(message = "Pickup city is required")
    private String pickupCity;

    @NotBlank(message = "Pickup state is required")
    private String pickupState;

    private String pickupStreet;
    private String pickupZip;
    private String pickupCountry;
    private String pickupLotNumber;
    private String pickupContactName;
    private String pickupContactPhone;

    @NotBlank(message = "Delivery city is required")
    private String dropCity;

    @NotBlank(message = "Delivery state is required")
    private String dropState;

    private String dropStreet;
    private String dropZip;
    private String dropCountry;
    private String dropLotNumber;
    private String dropContactName;
    private String dropContactPhone;

    private am.loadboardbackend.model.PickupType pickupType;
    private am.loadboardbackend.model.DropType dropType;

    @NotBlank(message = "Vehicle make is required")
    private String vehicleMake;

    @NotBlank(message = "Vehicle model is required")
    private String vehicleModel;

    @NotNull(message = "Vehicle year is required")
    @Min(value = 1900, message = "Vehicle year must be 1900 or later")
    @Max(value = 2100, message = "Vehicle year is not valid")
    private Integer vehicleYear;

    private String vehicleType;
    private String vehicleCondition;
    private String vin;
    private String trailerType;
    private String vehicleAdditionalInfo;

    private String description;
    private Double weight;
    private Double price;
    private LocalDate pickupDate;
    private String pickupTime;
    private LocalDate deliveryDate;
    private String deliveryTime;

    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String orderId;
    private String paymentMethod;
    private String paymentTiming;

    private List<AdditionalVehicleRequest> additionalVehicles;
}
