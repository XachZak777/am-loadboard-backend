package am.loadboardbackend.repository;

import am.loadboardbackend.model.BrokerValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrokerValidationRepository extends JpaRepository<BrokerValidation, UUID> {
}
