package am.loadboardbackend.dto.load;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateLoadRequest {
    /** Pickup address */
    private String pickupCity;
    private String pickupState;
    private String pickupStreet;
    private String pickupZip;
    private String pickupCountry;
    private String pickupLotNumber;

    /** Drop address */
    private String dropCity;
    private String dropState;
    private String dropStreet;
    private String dropZip;
    private String dropCountry;
    private String dropLotNumber;

    /** Pickup/Drop types */
    private am.loadboardbackend.model.PickupType pickupType;
    private am.loadboardbackend.model.DropType dropType;

    /** Vehicle info (auto-transport) */
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;

    private String description;
    private Double weight;
    private Double price;
    private LocalDate pickupDate;
    private LocalDate deliveryDate;
}
