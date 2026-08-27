package com.farmlink.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        response.put("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
