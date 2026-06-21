package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityToken extends Auditable<String> {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(40)")
    private TokenType tokenType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean used = false;

    @PrePersist
    protected void onCreate() {
        if (this.token == null) {
            this.token = UUID.randomUUID().toString();
        }
        this.used = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        EMAIL_CHANGE,
        LOGIN_CODE
    }
}
