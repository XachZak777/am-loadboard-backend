package am.loadboardbackend.repository;

import am.loadboardbackend.model.SecurityToken;
import am.loadboardbackend.model.SecurityToken.TokenType;
import am.loadboardbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SecurityTokenRepository extends JpaRepository<SecurityToken, UUID> {

    Optional<SecurityToken> findByToken(String token);

    Optional<SecurityToken> findByTokenAndTokenType(String token, TokenType tokenType);

    Optional<SecurityToken> findByTokenAndTokenTypeAndUser(String token, TokenType tokenType, User user);

    @Modifying
    @Query("DELETE FROM SecurityToken st WHERE st.user = :user AND st.tokenType = :tokenType")
    void deleteAllByUserAndTokenType(@Param("user") User user, @Param("tokenType") TokenType tokenType);

    @Modifying
    @Query("DELETE FROM SecurityToken st WHERE st.expiresAt < :now")
    void deleteAllExpired(@Param("now") LocalDateTime now);
}
