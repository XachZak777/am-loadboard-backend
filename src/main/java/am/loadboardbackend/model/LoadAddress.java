package am.loadboardbackend.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class LoadAddress {
    private String street;
    private String city;
    private String state;
    private String zip;
    private String country;

    private String lotNumber;
    private String contactName;
    private String contactPhone;
}
