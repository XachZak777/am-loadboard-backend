package am.loadboardbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
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

    // Profile completion fields (added for registration wizard steps 2-4)
    private String companyName;
    private String phoneNumber;
    private String mailingAddress;
    private String city;
    private String state;
    private String zipCode;

    @Column(name = "insurance_company")
    private String insuranceCompany;

    @Column(name = "cargo_insurance", precision = 15, scale = 2)
    private java.math.BigDecimal cargoInsurance;

    @Column(name = "liability_insurance", precision = 15, scale = 2)
    private java.math.BigDecimal liabilityInsurance;

    /**
     * 'EIN' or 'SSN'
     */
    @Column(name = "tax_id_type", length = 10,
            columnDefinition = "VARCHAR(10)")
    private String taxIdType;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    // Bond information (replaces insurance on the signup form)
    @Column(name = "bond_company")
    private String bondCompany;

    @Column(name = "bond_policy_number")
    private String bondPolicyNumber;

    @Column(name = "bond_coverage")
    private String bondCoverage;

    @Column(name = "bond_effective_date")
    private String bondEffectiveDate;

    @Column(name = "bond_agent_first_name")
    private String bondAgentFirstName;

    @Column(name = "bond_agent_last_name")
    private String bondAgentLastName;

    @Column(name = "bond_agent_email")
    private String bondAgentEmail;

    @Column(name = "bond_agent_phone")
    private String bondAgentPhone;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

