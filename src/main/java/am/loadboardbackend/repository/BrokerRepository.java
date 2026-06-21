package am.loadboardbackend.repository;

import am.loadboardbackend.model.Broker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrokerRepository extends JpaRepository<Broker, UUID> {
    Optional<Broker> findByMcNumber(String mcNumber);
    Optional<Broker> findByDotNumber(String dotNumber);

    @Query("SELECT b FROM Broker b WHERE " +
           "LOWER(COALESCE(b.companyName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(b.legalName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "COALESCE(b.dotNumber, '') LIKE CONCAT('%', :q, '%') OR " +
           "COALESCE(b.mcNumber, '') LIKE CONCAT('%', :q, '%')")
    List<Broker> searchByQuery(@Param("q") String q);
}
