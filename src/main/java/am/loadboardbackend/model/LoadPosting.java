package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

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

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "pickup_street")),
        @AttributeOverride(name = "city", column = @Column(name = "pickup_city")),
        @AttributeOverride(name = "state", column = @Column(name = "pickup_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "pickup_zip")),
        @AttributeOverride(name = "country", column = @Column(name = "pickup_country"))
    })
    private Address pickupAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "delivery_street")),
        @AttributeOverride(name = "city", column = @Column(name = "delivery_city")),
        @AttributeOverride(name = "state", column = @Column(name = "delivery_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "delivery_zip")),
        @AttributeOverride(name = "country", column = @Column(name = "delivery_country"))
    })
    private Address deliveryAddress;

    private String description;
    private Double weight;
    private Double price;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static enum LoadStatus { OPEN, ASSIGNED, CANCELLED, COMPLETED }
}
