package am.loadboardbackend.repository;

import am.loadboardbackend.model.Broker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BrokerRepository extends JpaRepository<Broker, UUID> {
    Optional<Broker> findByUserId(UUID userId);
    Optional<Broker> findByMcNumber(String mcNumber);
    Optional<Broker> findByDotNumber(String dotNumber);
}
