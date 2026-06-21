package am.loadboardbackend.repository;

import am.loadboardbackend.model.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DealerRepository extends JpaRepository<Dealer, UUID> {
}
