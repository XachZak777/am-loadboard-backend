package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = true)
    @JoinColumn(name = "carrier_id")
    private Carrier carrier;

    @OneToOne(optional = true)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
