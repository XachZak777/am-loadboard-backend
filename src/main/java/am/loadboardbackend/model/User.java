package am.loadboardbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
public class User extends Auditable<String> {

    @Id
    @GeneratedValue
    private UUID id;

    @ToString.Exclude
    @OneToOne(optional = true, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "carrier_id", nullable = true)
    private Carrier carrier;

    @ToString.Exclude
    @OneToOne(optional = true, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "broker_id", nullable = true)
    private Broker broker;

    @ToString.Exclude
    @OneToOne(optional = true, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dealer_id", nullable = true)
    private Dealer dealer;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
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

    /**
     * Manual admin approval flag. Until approved, the user can authenticate
     * but should not be able to access protected business endpoints.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean adminApproved = false;

    private LocalDateTime adminApprovedAt;

    /**
     * Set to true when an admin actively declines/rejects this registration.
     * Distinguishes "never reviewed" (pending) from "reviewed and rejected".
     * Reset to false when the user is subsequently approved.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean declined = false;

    private LocalDateTime declinedAt;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean loginDisabled = false;

    @Column(nullable = false, columnDefinition = "int not null default 0")
    private int failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;

    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<SecurityToken> securityTokens = new HashSet<>();
}
