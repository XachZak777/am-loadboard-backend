package am.loadboardbackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    /**
     * JWT access token (bearer).
     */
    private String token;

    /**
     * Authenticated user id.
     */
    private String userId;

    /**
     * Authenticated user email.
     */
    private String email;

    /**
     * Role name expected by the frontend: BROKER | CARRIER | ADMIN.
     */
    private String role;
}
