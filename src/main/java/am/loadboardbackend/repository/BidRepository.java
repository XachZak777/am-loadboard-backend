package am.loadboardbackend.repository;

import am.loadboardbackend.model.Bid;
import am.loadboardbackend.model.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findAllByLoadId(UUID loadId);
    List<Bid> findAllByCarrierId(UUID carrierId);
    void deleteAllByLoadId(UUID loadId);

    @Query("SELECT COUNT(DISTINCT b.load.id) FROM Bid b WHERE b.load.broker.id = :brokerId AND b.status = :status")
    long countByBrokerIdAndStatus(@Param("brokerId") UUID brokerId, @Param("status") BidStatus status);

    @Query("SELECT COUNT(DISTINCT b.load.id) FROM Bid b WHERE b.load.dealer.id = :dealerId AND b.status = :status")
    long countByDealerIdAndStatus(@Param("dealerId") UUID dealerId, @Param("status") BidStatus status);
}
