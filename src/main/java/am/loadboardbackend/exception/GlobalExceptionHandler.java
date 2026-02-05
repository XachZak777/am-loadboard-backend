package am.loadboardbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {

        HttpStatus status = mapStatus(ex.getMessage());

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", status.value(),
                        "error", ex.getMessage(),
                        "message", mapMessage(ex.getMessage())
                ));
    }

    private HttpStatus mapStatus(String code) {
        return switch (code) {
            case "CARRIER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CARRIER_NOT_ALLOWED" -> HttpStatus.FORBIDDEN;
            case "CARRIER_ALREADY_REGISTERED" -> HttpStatus.CONFLICT;
            case "EMAIL_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "FMCSA_SERVICE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String mapMessage(String code) {
        return switch (code) {
            case "CARRIER_NOT_FOUND" ->
                    "Carrier was not found in the FMCSA database";
            case "CARRIER_NOT_ALLOWED" ->
                    "Carrier is not allowed to operate";
            case "CARRIER_ALREADY_REGISTERED" ->
                    "Carrier is already registered";
            case "EMAIL_ALREADY_EXISTS" ->
                    "Email address is already in use";
            case "INVALID_CREDENTIALS" ->
                    "Invalid email or password";
            case "FMCSA_SERVICE_UNAVAILABLE" ->
                    "FMCSA service is temporarily unavailable";
            default ->
                    "Request could not be processed";
        };
    }
}

