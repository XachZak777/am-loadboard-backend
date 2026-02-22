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

    public BrokerResponseDto getMyBroker(UUID brokerId) {
        Broker b = repo.findById(brokerId).orElseThrow();

        return new BrokerResponseDto(
                b.getMcNumber(),
                b.getDotNumber(),
                b.getLegalName(),
                b.getOperatingStatus(),
                b.isBrokerAuthorityActive()
        );
    }
}