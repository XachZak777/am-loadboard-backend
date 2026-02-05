package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "carrier_id", nullable = false)
    private Carrier carrier;

    public enum Role {
        ROLE_CARRIER,
        ROLE_ADMIN
    }

    @Enumerated(EnumType.STRING)
    private Role role;

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
