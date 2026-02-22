package am.loadboardbackend.dto.validation;

import java.util.UUID;

public record SaveFromValidationRequest(
        UUID validationId,
        String email,
        String password
) {}
