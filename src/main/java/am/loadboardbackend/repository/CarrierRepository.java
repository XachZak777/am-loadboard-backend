package am.loadboardbackend.repository;

import am.loadboardbackend.model.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarrierRepository extends JpaRepository<Carrier, UUID> {
    Optional<Carrier> findByDotNumber(String dotNumber);
    Optional<Carrier> findByMcNumber(String mcNumber);

    @Query("SELECT c FROM Carrier c WHERE " +
           "LOWER(COALESCE(c.companyName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(c.legalName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "COALESCE(c.dotNumber, '') LIKE CONCAT('%', :q, '%') OR " +
           "COALESCE(c.mcNumber, '') LIKE CONCAT('%', :q, '%')")
    List<Carrier> searchByQuery(@Param("q") String q);
}
