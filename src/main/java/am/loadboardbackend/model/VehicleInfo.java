package am.loadboardbackend.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class VehicleInfo {
    private String make;
    private String model;
    private Integer year;
    private String vehicleType;
    private String condition;
    private String vin;
    private String trailerType;
    private String additionalInfo;
}
