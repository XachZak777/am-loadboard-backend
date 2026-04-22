package am.loadboardbackend.dto;

/**
 * Standard error response shape used by all API endpoints.
 * Frontend GlobalExceptionHandler expects: status, error, message, path.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path
) {
    /**
     * Legacy constructor kept for backward compatibility with existing handlers
     * that pass (timestamp, status, error, message).
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path);
    }
}

