package am.loadboardbackend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import am.loadboardbackend.dto.ErrorResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ErrorResponse body = ErrorResponse.of(
                ex.getStatusCode().value(),
                HttpStatus.resolve(ex.getStatusCode().value()) != null
                        ? HttpStatus.resolve(ex.getStatusCode().value()).getReasonPhrase()
                        : "Error",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException ex, HttpServletRequest request) {
        HttpStatus status = mapStatus(ex.getMessage());
        if (status.is5xxServerError()) {
            log.error("Unhandled runtime exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        }
        ErrorResponse body = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                mapMessage(ex.getMessage()),
                request.getRequestURI()
        );
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler({HttpMediaTypeNotAcceptableException.class, HttpMessageNotWritableException.class})
    public ResponseEntity<ErrorResponse> handleSerializationErrors(
            Exception ex, HttpServletRequest request) {
        log.warn("Serialization error at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Serialization Error",
                "Response could not be serialized.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Resource was modified by another process; please retry",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String detail = extractDetail(ex);
        String message = "Data integrity violation";
        if (detail.contains("loads_status_check")) {
            message = "Load status update is not allowed by the current schema — run the V2 migration.";
        } else if ((detail.contains("Key (email)") || detail.contains("users_email")) && !detail.contains("contact_email")) {
            message = "Email address is already in use";
        } else if (detail.contains("contact_email")) {
            message = "Contact email is already associated with another load. Please use a different contact email.";
        } else if (detail.contains("carrier_id") && detail.contains("relation \"loads\"")) {
            message = "Load posting failed: database schema is out of date.";
        } else if (detail.contains("carrier_id")) {
            message = "Registration failed: carrier association is missing.";
        } else if (detail.contains("broker_id")) {
            message = "Registration failed: broker association is missing.";
        } else if (detail.contains("dealer_id")) {
            message = "Registration failed: dealer association is missing.";
        } else if (detail.contains("mc_number")) {
            message = "An account with this MC number already exists";
        } else if (detail.contains("dot_number")) {
            message = "An account with this DOT number already exists";
        }
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private static String extractDetail(DataIntegrityViolationException ex) {
        try {
            Throwable t = ex;
            for (int i = 0; i < 8 && t != null; i++) {
                if (t.getMessage() != null) return t.getMessage();
                t = t.getCause();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private HttpStatus mapStatus(String code) {
        if (code == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        return switch (code) {
            case "CARRIER_NOT_ALLOWED" -> HttpStatus.FORBIDDEN;
            case "CARRIER_ALREADY_REGISTERED", "EMAIL_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String mapMessage(String code) {
        if (code == null) return "Unexpected error";
        return switch (code) {
            case "CARRIER_NOT_ALLOWED" -> "Carrier is not allowed to operate";
            case "CARRIER_ALREADY_REGISTERED" -> "Carrier is already registered";
            case "EMAIL_ALREADY_EXISTS" -> "Email address is already in use";
            case "INVALID_CREDENTIALS" -> "Invalid email or password";
            default -> "Request could not be processed";
        };
    }
}
