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

    @ManyToOne(optional = false)
    @JoinColumn(name = "carrier_id", nullable = false)
    private Carrier carrier;

    private String pickupCity;
    private String pickupState;
    private String deliveryCity;
    private String deliveryState;
    private String description;
    private Double weight;
    private Double price;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
