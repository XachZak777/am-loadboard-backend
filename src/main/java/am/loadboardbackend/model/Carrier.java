package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "carriers")
@Data
public class Carrier {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String dotNumber;

    @Column(unique = true)
    private String mcNumber;

    private String legalName;
    private String dbaName;

    private String operatingStatus;
    private String safetyRating;

    private boolean verified;
    private Boolean subscriptionActive = Boolean.FALSE;

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
