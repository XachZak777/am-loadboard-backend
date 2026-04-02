package am.loadboardbackend.model;

import am.loadboardbackend.dto.carrier.CarrierLookupType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "carrier_validations")
@Data
public class CarrierValidation {

    @Id
    @GeneratedValue
    private UUID id;
    @Version
    private Long version;

    private String lookupValue;

    @Enumerated(EnumType.STRING)
    private CarrierLookupType lookupType;

    private String dotNumber;
    private String mcNumber;
    private String legalName;
    private String dbaName;
    private String operatingStatus;
    private String allowedToOperate;

    private String phyStreet;
    private String phyCity;
    private String phyState;
    private String phyZip;
    private String phyCountry;

    private Integer totalDrivers;
    private Integer totalPowerUnits;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
