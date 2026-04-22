package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loads")
@Data
public class LoadPosting {

    @Id
    @GeneratedValue
    private UUID id;

    // The broker who posted the load
    @ManyToOne(optional = false)
    @JoinColumn(name = "broker_id", nullable = false)
    private Broker broker;

    // Once assigned, this references the carrier who accepted/was approved
    @ManyToOne
    @JoinColumn(name = "assigned_carrier_id")
    private Carrier assignedCarrier;

    @Enumerated(EnumType.STRING)
    private LoadStatus status = LoadStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_type")
    private PickupType pickupType;

    @Enumerated(EnumType.STRING)
    @Column(name = "drop_type")
    private DropType dropType;

    /**
     * Pickup address for the load (separated from other address usage like Carrier physical address).
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "pickup_street")),
        @AttributeOverride(name = "city", column = @Column(name = "pickup_city")),
        @AttributeOverride(name = "state", column = @Column(name = "pickup_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "pickup_zip")),
        @AttributeOverride(name = "country", column = @Column(name = "pickup_country")),
        @AttributeOverride(name = "lotNumber", column = @Column(name = "pickup_lot_number"))
    })
    private LoadAddress pickupAddress;

    /**
     * Drop/delivery address for the load.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "drop_street")),
        @AttributeOverride(name = "city", column = @Column(name = "drop_city")),
        @AttributeOverride(name = "state", column = @Column(name = "drop_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "drop_zip")),
        @AttributeOverride(name = "country", column = @Column(name = "drop_country")),
        @AttributeOverride(name = "lotNumber", column = @Column(name = "drop_lot_number"))
    })
    private LoadAddress dropAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "make", column = @Column(name = "vehicle_make")),
        @AttributeOverride(name = "model", column = @Column(name = "vehicle_model")),
        @AttributeOverride(name = "year", column = @Column(name = "vehicle_year"))
    })
    private VehicleInfo vehicle;

    private String description;
    private Double weight;
    private Double price;

    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static enum LoadStatus { OPEN, ASSIGNED, CANCELLED, COMPLETED }
}
