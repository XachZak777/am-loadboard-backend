package am.loadboardbackend.repository;

import am.loadboardbackend.model.AiChatUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiChatUsageRepository extends JpaRepository<AiChatUsage, UUID> {
}
