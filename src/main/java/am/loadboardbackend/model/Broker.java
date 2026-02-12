package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "brokers")
@Data
public class Broker {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String mcNumber;

    @Column(unique = true)
    private String dotNumber;

    private String legalName;
    private String operatingStatus;
    private boolean brokerAuthorityActive;

    @Column(nullable = false)
    private UUID userId;

    @Column(columnDefinition = "jsonb")
    private String rawFmcsa;
}
