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

    @ManyToOne(optional = true)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    @ManyToOne(optional = true)
    @JoinColumn(name = "dealer_id")
    private Dealer dealer;

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

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "pickup_street")),
        @AttributeOverride(name = "city", column = @Column(name = "pickup_city")),
        @AttributeOverride(name = "state", column = @Column(name = "pickup_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "pickup_zip")),
        @AttributeOverride(name = "country", column = @Column(name = "pickup_country")),
        @AttributeOverride(name = "lotNumber", column = @Column(name = "pickup_lot_number")),
        @AttributeOverride(name = "contactName", column = @Column(name = "pickup_contact_name")),
        @AttributeOverride(name = "contactPhone", column = @Column(name = "pickup_contact_phone"))
    })
    private LoadAddress pickupAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "drop_street")),
        @AttributeOverride(name = "city", column = @Column(name = "drop_city")),
        @AttributeOverride(name = "state", column = @Column(name = "drop_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "drop_zip")),
        @AttributeOverride(name = "country", column = @Column(name = "drop_country")),
        @AttributeOverride(name = "lotNumber", column = @Column(name = "drop_lot_number")),
        @AttributeOverride(name = "contactName", column = @Column(name = "drop_contact_name")),
        @AttributeOverride(name = "contactPhone", column = @Column(name = "drop_contact_phone"))
    })
    private LoadAddress dropAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "make", column = @Column(name = "vehicle_make")),
        @AttributeOverride(name = "model", column = @Column(name = "vehicle_model")),
        @AttributeOverride(name = "year", column = @Column(name = "vehicle_year")),
        @AttributeOverride(name = "vehicleType", column = @Column(name = "vehicle_type")),
        @AttributeOverride(name = "condition", column = @Column(name = "vehicle_condition")),
        @AttributeOverride(name = "vin", column = @Column(name = "vehicle_vin")),
        @AttributeOverride(name = "trailerType", column = @Column(name = "trailer_type")),
        @AttributeOverride(name = "additionalInfo", column = @Column(name = "vehicle_additional_info"))
    })
    private VehicleInfo vehicle;

    private String description;
    private Double weight;
    private Double price;
    private Double distance;

    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    @Column(name = "pickup_time")
    private String pickupTime;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "delivery_time")
    private String deliveryTime;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_timing")
    private String paymentTiming;

    @Column(name = "additional_vehicles", columnDefinition = "TEXT")
    private String additionalVehicles;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static enum LoadStatus { OPEN, ASSIGNED, PICKED_UP, DELIVERED, PAID, CANCELLED, COMPLETED }
}
