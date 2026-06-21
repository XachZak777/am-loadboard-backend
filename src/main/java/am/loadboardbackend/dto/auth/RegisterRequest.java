package am.loadboardbackend.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    /**
     * Expected values from frontend: BROKER | CARRIER
     */
    private String role;
}
