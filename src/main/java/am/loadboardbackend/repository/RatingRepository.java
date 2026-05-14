package am.loadboardbackend.repository;

import am.loadboardbackend.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    List<Rating> findAllByTargetIdAndTargetType(UUID targetId, String targetType);
    boolean existsByLoadIdAndSubmitterId(UUID loadId, UUID submitterId);
    long countByTargetIdAndTargetTypeAndType(UUID targetId, String targetType, String type);
    List<Rating> findAllBySubmitterId(UUID submitterId);
}
