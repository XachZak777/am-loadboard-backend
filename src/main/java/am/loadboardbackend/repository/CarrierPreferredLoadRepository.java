package am.loadboardbackend.repository;

import am.loadboardbackend.model.CarrierPreferredLoad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarrierPreferredLoadRepository extends JpaRepository<CarrierPreferredLoad, UUID> {
    List<CarrierPreferredLoad> findAllByCarrierIdOrderBySavedAtDesc(UUID carrierId);
    Optional<CarrierPreferredLoad> findByCarrierIdAndLoadId(UUID carrierId, UUID loadId);
    boolean existsByCarrierIdAndLoadId(UUID carrierId, UUID loadId);
    void deleteByCarrierIdAndLoadId(UUID carrierId, UUID loadId);
    List<UUID> findLoadIdsByCarrierId(UUID carrierId);
}
