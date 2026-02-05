package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "carriers")
public class Carrier {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private Long dotNumber;

    @Column(unique = true)
    private String mcNumber;

    private String legalName;
    private String dbaName;

    private String operatingStatus;
    private String safetyRating;

    private Boolean verified;

    private String phyStreet;
    private String phyCity;
    private String phyState;
    private String phyZip;
    private String phyCountry;

    private Integer totalDrivers;
    private Integer totalPowerUnits;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String rawFmcsa;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
