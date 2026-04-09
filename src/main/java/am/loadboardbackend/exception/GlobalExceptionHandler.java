package am.loadboardbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import am.loadboardbackend.dto.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResponseStatusException.class)
        @org.springframework.web.bind.annotation.ResponseBody
        public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
                ErrorResponse body = new ErrorResponse(
                                LocalDateTime.now().toString(),
                                ex.getStatusCode().value(),
                                ex.getReason(),
                                ex.getReason() == null ? ex.getMessage() : ex.getReason()
                );
                return ResponseEntity
                                .status(ex.getStatusCode())
                                .body(body);
        }

    @ExceptionHandler(RuntimeException.class)
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        HttpStatus status = mapStatus(ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now().toString(),
                status.value(),
                ex.getMessage(),
                mapMessage(ex.getMessage())
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now().toString(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                ex.getMessage() == null ? "Unexpected error" : ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    @ExceptionHandler({HttpMediaTypeNotAcceptableException.class, HttpMessageNotWritableException.class})
    public ResponseEntity<ErrorResponse> handleSerializationErrors(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                java.time.LocalDateTime.now().toString(),
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "SERIALIZATION_ERROR",
                ex.getMessage()
        );
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now().toString(),
                org.springframework.http.HttpStatus.CONFLICT.value(),
                "RESOURCE_CONFLICT",
                "Resource was modified by another process; please retry"
        );
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                .body(body);
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

