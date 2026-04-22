package am.loadboardbackend.dto.auth;

/**
 * Response for GET /api/auth/me — tells the frontend what state the user is in.
 */
public record MeResponse(
        String userId,
        String email,
        String role,
        boolean adminApproved,
        boolean emailVerified,
        boolean profileComplete
) {}
