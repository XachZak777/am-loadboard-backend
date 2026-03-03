package am.loadboardbackend.repository;

import am.loadboardbackend.model.LoadPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoadPostingRepository extends JpaRepository<LoadPosting, UUID> {
    List<LoadPosting> findAllByCarrierId(UUID carrierId);
}
