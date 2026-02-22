package am.loadboardbackend.dto.validation;

import java.util.UUID;

public record RegisterFromValidationRequest(
        UUID validationId,
        String email,
        String password
) {}
