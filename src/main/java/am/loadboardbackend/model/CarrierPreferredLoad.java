package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "carrier_preferred_loads",
    uniqueConstraints = @UniqueConstraint(columnNames = {"carrier_id", "load_id"})
)
@Getter @Setter @NoArgsConstructor
public class CarrierPreferredLoad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "carrier_id", nullable = false)
    private UUID carrierId;

    @Column(name = "load_id", nullable = false)
    private UUID loadId;

    @Column(nullable = false)
    private LocalDateTime savedAt = LocalDateTime.now();
}
