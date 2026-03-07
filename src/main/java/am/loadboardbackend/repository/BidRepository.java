package am.loadboardbackend.repository;

import am.loadboardbackend.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findAllByLoadId(UUID loadId);
}
