package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dealers")
@Data
public class Dealer {

    @Id
    @GeneratedValue
    private UUID id;

    private String companyName;
    private String ownerFirstName;
    private String ownerLastName;
    private String businessPhone;
    private String companyAddress;
    private String city;
    private String state;
    private String zipCode;
    private String yearEstablished;
    private String dealerLicenseNumber;
    private String auctionAccessNumber;
    private String howDidYouHear;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
