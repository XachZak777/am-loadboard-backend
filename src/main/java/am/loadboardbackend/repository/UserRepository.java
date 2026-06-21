package am.loadboardbackend.repository;

import am.loadboardbackend.model.User;
import am.loadboardbackend.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    long countByRole(UserRole role);
    Optional<User> findByCarrierId(UUID carrierId);
    Optional<User> findByBrokerId(UUID brokerId);
    Optional<User> findByDealerId(UUID dealerId);
    List<User> findByRoleNot(UserRole role);
    List<User> findByRoleNotAndAdminApprovedTrue(UserRole role);
    List<User> findByRoleNotAndAdminApprovedFalseAndDeclinedFalse(UserRole role);
    List<User> findByRoleNotAndDeclinedTrue(UserRole role);

    // Eagerly fetch all profile associations in one query to avoid N+1 selects
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.carrier LEFT JOIN FETCH u.broker LEFT JOIN FETCH u.dealer WHERE u.role <> :role")
    List<User> findByRoleNotWithProfiles(@Param("role") UserRole role);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.carrier LEFT JOIN FETCH u.broker LEFT JOIN FETCH u.dealer WHERE u.role <> :role AND u.adminApproved = true")
    List<User> findByRoleNotAndAdminApprovedTrueWithProfiles(@Param("role") UserRole role);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.carrier LEFT JOIN FETCH u.broker LEFT JOIN FETCH u.dealer WHERE u.role <> :role AND u.adminApproved = false AND u.declined = false")
    List<User> findByRoleNotAndAdminApprovedFalseAndDeclinedFalseWithProfiles(@Param("role") UserRole role);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.carrier LEFT JOIN FETCH u.broker LEFT JOIN FETCH u.dealer WHERE u.role <> :role AND u.declined = true")
    List<User> findByRoleNotAndDeclinedTrueWithProfiles(@Param("role") UserRole role);

    // Used by startup data-repair to find users registered before stub creation was added
    List<User> findByRoleAndCarrierIsNull(UserRole role);
    List<User> findByRoleAndBrokerIsNull(UserRole role);
}
