package com.farmlink.api.exception;

import com.farmlink.api.router.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException ex) {
        logger.debug("Static resource not found: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Requested resource not found.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid API request argument: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage() != null ? ex.getMessage() : "Invalid parameter supplied.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.warn("Validation error on request: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        response.put("error", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElementException(NoSuchElementException ex) {
        logger.warn("Requested resource not found: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage() != null ? ex.getMessage() : "Requested resource not found.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(FirestoreQuotaExhaustedException.class)
    public ResponseEntity<Map<String, String>> handleFirestoreQuotaExhaustedException(FirestoreQuotaExhaustedException ex) {
        logger.warn("Firestore quota circuit breaker active: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Live market discovery is temporarily unavailable due to database quota limits. Please retry shortly.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(com.google.api.gax.rpc.ResourceExhaustedException.class)
    public ResponseEntity<Map<String, String>> handleResourceExhaustedException(com.google.api.gax.rpc.ResourceExhaustedException ex) {
        logger.error("Firestore read quota exceeded: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Live market discovery is temporarily unavailable. Please retry.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(io.grpc.StatusRuntimeException.class)
    public ResponseEntity<Map<String, String>> handleStatusRuntimeException(io.grpc.StatusRuntimeException ex) {
        if (ex.getStatus() != null && ex.getStatus().getCode() == io.grpc.Status.Code.RESOURCE_EXHAUSTED) {
            logger.error("gRPC Firestore RESOURCE_EXHAUSTED error: {}", ex.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", "Live market discovery is temporarily unavailable. Please retry.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        logger.error("gRPC StatusRuntimeException: {}", ex.getMessage(), ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Live market telemetry service unavailable. Please retry.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<Map<String, String>> handleInvalidQueryException(InvalidQueryException ex) {
        logger.warn("Invalid router query: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UnauthorizedEconomicsAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedEconomicsAccessException(UnauthorizedEconomicsAccessException ex) {
        logger.warn("Unauthorized economics access: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSessionNotFoundException(SessionNotFoundException ex) {
        logger.warn("Session not found: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(TurnOutOfSequenceException.class)
    public ResponseEntity<Map<String, String>> handleTurnOutOfSequenceException(TurnOutOfSequenceException ex) {
        logger.warn("Turn out of sequence: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, String>> handleIdempotencyConflictException(IdempotencyConflictException ex) {
        logger.warn("Idempotency conflict: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(TurnResultExpiredException.class)
    public ResponseEntity<Map<String, String>> handleTurnResultExpiredException(TurnResultExpiredException ex) {
        logger.warn("Turn result expired: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE).body(response);
    }

    @ExceptionHandler(SessionLockedException.class)
    public ResponseEntity<Map<String, String>> handleSessionLockedException(SessionLockedException ex) {
        logger.warn("Session locked: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED).body(response);
    }

    @ExceptionHandler(RedisCoordinationException.class)
    public ResponseEntity<Map<String, String>> handleRedisCoordinationException(RedisCoordinationException ex) {
        logger.error("Redis coordination error: {}", ex.getMessage(), ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Session coordination service is temporarily unavailable. Please retry.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(SessionCorruptedException.class)
    public ResponseEntity<Map<String, String>> handleSessionCorruptedException(SessionCorruptedException ex) {
        logger.error("Session corrupted: {}", ex.getMessage(), ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Session state corrupted. Please initialize a new session.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("RESOURCE_EXHAUSTED") || msg.contains("Quota exceeded")) {
            logger.error("Firestore Quota Exceeded Exception: {}", msg);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Live market discovery is temporarily unavailable. Please retry.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        logger.error("Unhandled API exception occurred: {}", ex.getMessage(), ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "An unexpected server error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
