package am.loadboardbackend.repository;

import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    long countByRole(UserRole role);
}
