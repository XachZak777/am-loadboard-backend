package am.loadboardbackend.repository;

import am.loadboardbackend.model.CarrierValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarrierValidationRepository extends JpaRepository<CarrierValidation, UUID> {
}
