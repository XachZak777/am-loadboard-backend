package am.loadboardbackend.dto.auth;

public record ResetPasswordRequest(String token, String newPassword) {}
