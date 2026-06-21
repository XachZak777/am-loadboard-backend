package am.loadboardbackend.service;

import am.loadboardbackend.dto.broker.BrokerResponseDto;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.repository.BrokerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrokerService {

    private final BrokerRepository repo;

    public BrokerResponseDto getMyBroker(UUID brokerId) {
        Broker b = repo.findById(brokerId).orElseThrow();

        return new BrokerResponseDto(
                b.getMcNumber(),
                b.getDotNumber(),
                b.getLegalName(),
                b.getCompanyName(),
                b.getOperatingStatus(),
                b.isBrokerAuthorityActive(),
                b.getPhoneNumber(),
                b.getMailingAddress(),
                b.getCity(),
                b.getState(),
                b.getZipCode(),
                b.getInsuranceCompany(),
                b.getCargoInsurance(),
                b.getLiabilityInsurance(),
                b.getTaxIdType(),
                b.getTaxId(),
                b.getBondCompany(),
                b.getBondPolicyNumber(),
                b.getBondCoverage(),
                b.getBondEffectiveDate(),
                b.getBondAgentFirstName(),
                b.getBondAgentLastName(),
                b.getBondAgentEmail(),
                b.getBondAgentPhone()
        );
    }

    public Broker getEntity(UUID brokerId) {
        return repo.findById(brokerId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Broker not found"));
    }

    public List<Broker> search(String query) {
        return repo.searchByQuery(query);
    }
}