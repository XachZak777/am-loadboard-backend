package am.loadboardbackend.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class VehicleInfo {
    private String make;
    private String model;
    private Integer year;
}
