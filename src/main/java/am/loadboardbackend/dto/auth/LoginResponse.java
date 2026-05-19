package am.loadboardbackend.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

    /**
     * Whether the account has been approved by an admin.
     * The frontend uses this to show a "pending approval" screen.
     */
    private boolean adminApproved;

    /**
     * Company / legal name of the authenticated user's profile.
     * Null for admin accounts.
     */
    private String companyName;

    public LoginResponse(String token, String userId, String email, String role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.adminApproved = false;
    }

    public LoginResponse(String token, String userId, String email, String role, boolean adminApproved) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.adminApproved = adminApproved;
    }

    public LoginResponse(String token, String userId, String email, String role, boolean adminApproved, String companyName) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.adminApproved = adminApproved;
        this.companyName = companyName;
    }
}
