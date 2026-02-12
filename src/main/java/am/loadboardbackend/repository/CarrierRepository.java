package am.loadboardbackend.repository;

import am.loadboardbackend.model.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CarrierRepository extends JpaRepository<Carrier, UUID> {
    Optional<Carrier> findByDotNumber(String dotNumber);
    Optional<Carrier> findByMcNumber(String mcNumber);
}
