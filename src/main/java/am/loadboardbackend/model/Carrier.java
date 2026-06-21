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

    // Profile completion fields (added for registration wizard steps 2-4)
    private String companyName;
    private String phoneNumber;
    private String mailingAddress;
    private String city;
    private String state;
    private String zipCode;

    @Column(name = "insurance_company")
    private String insuranceCompany;

    @Column(name = "cargo_insurance", precision = 15, scale = 2)
    private java.math.BigDecimal cargoInsurance;

    @Column(name = "liability_insurance", precision = 15, scale = 2)
    private java.math.BigDecimal liabilityInsurance;

    /**
     * 'EIN' or 'SSN'
     * columnDefinition avoids NOT NULL without DEFAULT on ALTER TABLE ADD COLUMN
     */
    @Column(name = "tax_id_type", length = 10,
            columnDefinition = "VARCHAR(10)")
    private String taxIdType;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Column(name = "preferred_lines", columnDefinition = "TEXT")
    private String preferredLines;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
