package am.loadboardbackend.service;

import am.loadboardbackend.dto.broker.BrokerResponseDto;
import am.loadboardbackend.model.Broker;
import am.loadboardbackend.repository.BrokerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrokerService {

    private final BrokerRepository repo;

    public Broker save(BrokerResponseDto dto, UUID userId) {

        Broker broker = new Broker();
        broker.setUserId(userId);
        broker.setMcNumber(dto.mcNumber());
        broker.setDotNumber(dto.dotNumber());
        broker.setLegalName(dto.legalName());
        broker.setOperatingStatus(dto.operatingStatus());
        broker.setBrokerAuthorityActive(dto.brokerAuthorityActive());

        return repo.save(broker);
    }

    public BrokerResponseDto getMyBroker(UUID userId) {
        Broker b = repo.findByUserId(userId).orElseThrow();

        return new BrokerResponseDto(
                b.getMcNumber(),
                b.getDotNumber(),
                b.getLegalName(),
                b.getOperatingStatus(),
                b.isBrokerAuthorityActive()
        );
    }
}