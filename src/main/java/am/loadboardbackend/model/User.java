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

    @OneToOne(optional = true, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "carrier_id", nullable = true)
    private Carrier carrier;

    @OneToOne(optional = true, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "broker_id", nullable = true)
    private Broker broker;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    /**
     * Whether the user's email is verified. This is used to gate sensitive flows
     * like password reset.
     * columnDefinition supplies a DB-level DEFAULT so that ALTER TABLE … ADD COLUMN
     * succeeds even when existing rows are present.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean emailVerified = false;

    private LocalDateTime emailVerifiedAt;

    private LocalDateTime createdAt;

    /**
     * Manual admin approval flag. Until approved, the user can authenticate
     * but should not be able to access protected business endpoints.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean adminApproved = false;

    private LocalDateTime adminApprovedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
