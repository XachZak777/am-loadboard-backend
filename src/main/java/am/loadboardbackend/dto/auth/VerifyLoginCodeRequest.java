package am.loadboardbackend.dto.auth;

public record VerifyLoginCodeRequest(String email, String code) {}
