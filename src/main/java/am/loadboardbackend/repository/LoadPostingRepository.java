package am.loadboardbackend.repository;

import am.loadboardbackend.model.LoadPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LoadPostingRepository extends JpaRepository<LoadPosting, UUID> {
    List<LoadPosting> findAllByBrokerId(UUID brokerId);
    List<LoadPosting> findAllByDealerId(UUID dealerId);
    List<LoadPosting> findAllByStatus(am.loadboardbackend.model.LoadPosting.LoadStatus status);

    @Query("SELECT COUNT(l) FROM LoadPosting l WHERE l.broker.id = :brokerId AND l.status IN :statuses")
    long countByBrokerIdAndStatusIn(@Param("brokerId") UUID brokerId, @Param("statuses") List<LoadPosting.LoadStatus> statuses);

    @Query("SELECT COUNT(l) FROM LoadPosting l WHERE l.dealer.id = :dealerId AND l.status IN :statuses")
    long countByDealerIdAndStatusIn(@Param("dealerId") UUID dealerId, @Param("statuses") List<LoadPosting.LoadStatus> statuses);

    @Query("SELECT COUNT(l) FROM LoadPosting l WHERE l.assignedCarrier.id = :carrierId AND l.status = :status")
    long countByAssignedCarrierIdAndStatus(@Param("carrierId") UUID carrierId, @Param("status") LoadPosting.LoadStatus status);

    @Query("SELECT l FROM LoadPosting l WHERE l.assignedCarrier.id = :carrierId AND l.status IN :statuses")
    List<LoadPosting> findByAssignedCarrierIdAndStatusIn(@Param("carrierId") UUID carrierId, @Param("statuses") List<LoadPosting.LoadStatus> statuses);
}
