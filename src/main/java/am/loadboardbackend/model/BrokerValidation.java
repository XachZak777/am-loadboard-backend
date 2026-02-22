package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "broker_validations")
@Data
public class BrokerValidation {

    @Id
    @GeneratedValue
    private UUID id;

    private String mcNumber;
    private String dotNumber;
    private String legalName;
    private String operatingStatus;
    private boolean brokerAuthorityActive;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
