package am.loadboardbackend.repository;

import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    long countByRole(UserRole role);
    Optional<User> findByCarrierId(UUID carrierId);
    Optional<User> findByBrokerId(UUID brokerId);
    List<User> findByRoleNot(UserRole role);
    List<User> findByRoleNotAndAdminApprovedTrue(UserRole role);
    List<User> findByRoleNotAndAdminApprovedFalseAndDeclinedFalse(UserRole role);
    List<User> findByRoleNotAndDeclinedTrue(UserRole role);
}
