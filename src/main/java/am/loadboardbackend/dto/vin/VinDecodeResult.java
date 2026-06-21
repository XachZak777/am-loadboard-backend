package am.loadboardbackend.dto.vin;

import lombok.Data;

@Data
public class VinDecodeResult {
    // Identity
    private String  vin;
    private String  make;
    private String  model;
    private Integer year;
    private String  manufacturer;

    // Classification
    private String vehicleType;   // mapped to our system: sedan/suv/truck/van/motorcycle/rv/boat/atv
    private String bodyClass;     // raw NHTSA value e.g. "Coupe"
    private String trim;

    // Engine
    private String engineHp;
    private String cylinders;
    private String displacementL;
    private String engineConfiguration;
    private String engineModel;
    private String turbo;

    // Drivetrain & Transmission
    private String  driveType;
    private String  transmissionStyle;
    private String  transmissionSpeeds;

    // Fuel
    private String fuelType;

    // Dimensions & Weight
    private Integer doors;
    private Integer seats;
    private String  wheelbase;
    private Integer wheels;
    private String  gvwr;

    // Origin
    private String plantCountry;
    private String plantState;
    private String plantCity;

    // Market
    private String basePrice;
    private String steeringLocation;

    // Result
    private boolean success;
    private String  errorText;
}
