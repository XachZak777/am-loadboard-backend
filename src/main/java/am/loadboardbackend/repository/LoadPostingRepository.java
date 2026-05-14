package am.loadboardbackend.repository;

import am.loadboardbackend.model.LoadPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoadPostingRepository extends JpaRepository<LoadPosting, UUID> {
    List<LoadPosting> findAllByBrokerId(UUID brokerId);
    List<LoadPosting> findAllByDealerId(UUID dealerId);
    List<LoadPosting> findAllByStatus(am.loadboardbackend.model.LoadPosting.LoadStatus status);
}
