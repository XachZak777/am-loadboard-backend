package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bids")
@Data
public class Bid {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "load_id", nullable = false)
    private LoadPosting load;

    @ManyToOne(optional = false)
    @JoinColumn(name = "carrier_id", nullable = false)
    private Carrier carrier;

    private BigDecimal amount;

    private boolean bookNow;

    private LocalDate requestedPickupDate;
    private String requestedPickupTime;
    private LocalDate requestedDropDate;
    private String requestedDropTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    private BidStatus status = BidStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

}
