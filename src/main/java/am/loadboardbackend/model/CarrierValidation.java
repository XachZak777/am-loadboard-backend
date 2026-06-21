package am.loadboardbackend.model;

import am.loadboardbackend.dto.carrier.CarrierLookupType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
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

    // Identity
    private String dotNumber;
    private String mcNumber;
    private String legalName;
    private String dbaName;
    private String entityType;

    // Status
    private String operatingStatus;
    private String allowedToOperate;
    private String outOfServiceDate;
    private String latestUpdate;

    // Physical address
    private String phyStreet;
    private String phyCity;
    private String phyState;
    private String phyZip;
    private String phyCountry;

    // Mailing address
    private String mailingStreet;
    private String mailingCity;
    private String mailingState;
    private String mailingZip;
    private String mailingCountry;

    // Contact / Fleet
    private String phone;
    private Integer totalDrivers;
    private Integer totalPowerUnits;

    // Operation (stored as JSON arrays)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> operationClassification;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> carrierOperation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> cargoCarried;

    // MCS-150
    private String mcs150Date;
    private Integer mcs150Mileage;
    private Integer mcs150Year;

    // Safety
    private String safetyRating;
    private String safetyRatingDate;
    private String safetyReviewDate;
    private String safetyType;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
