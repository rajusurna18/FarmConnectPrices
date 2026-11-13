package com.farmlink.api.controller;

import com.farmlink.api.dto.CreateDiagnosticRequest;
import com.farmlink.api.dto.DiagnosticPageResponse;
import com.farmlink.api.dto.DiagnosticRecordResponse;
import com.farmlink.api.exception.DuplicateRecordException;
import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import com.farmlink.api.exception.InvalidImageSignatureException;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.CropDiagnosticService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/diagnostics")
public class CropDiagnosticController {

    private static final Logger logger = LoggerFactory.getLogger(CropDiagnosticController.class);

    private final CropDiagnosticService diagnosticService;

    public CropDiagnosticController(CropDiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    /**
     * Persist a new crop diagnostic record.
     * Atomic, create-only, strictly verified against authoritatively stored Storage objects.
     */
    @PostMapping
    public ResponseEntity<?> createDiagnostic(
            @Valid @RequestBody CreateDiagnosticRequest request,
            BindingResult bindingResult
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication token is required."));
        }

        if (bindingResult != null && bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .findFirst()
                    .orElse("Invalid request payload.");
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
        }

        try {
            DiagnosticRecordResponse response = diagnosticService.createDiagnostic(token.getUid(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DuplicateRecordException e) {
            logger.warn("Diagnostic creation conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (InvalidImageSignatureException e) {
            logger.warn("Invalid diagnostic image signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid diagnostic creation payload or image: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            logger.warn("Access denied during diagnostic creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            logger.warn("Referenced resource not found during diagnostic creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (FirestoreQuotaExhaustedException e) {
            logger.error("Firestore quota exhausted during diagnostic creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error persisting diagnostic: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to persist diagnostic record: " + e.getMessage()));
        }
    }

    /**
     * Retrieve a specific diagnostic record by ID, scoped to the owning farmer.
     */
    @GetMapping("/{diagnosticId}")
    public ResponseEntity<?> getDiagnosticById(@PathVariable String diagnosticId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication token is required."));
        }

        try {
            DiagnosticRecordResponse response = diagnosticService.getDiagnosticById(token.getUid(), diagnosticId);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            logger.warn("Access denied reading diagnostic {}: {}", diagnosticId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            logger.warn("Diagnostic {} not found: {}", diagnosticId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (FirestoreQuotaExhaustedException e) {
            logger.error("Firestore quota exhausted reading diagnostic {}: {}", diagnosticId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error fetching diagnostic {}: {}", diagnosticId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch diagnostic record: " + e.getMessage()));
        }
    }

    /**
     * List diagnostic records for the authenticated farmer with HMAC-signed cursor pagination.
     */
    @GetMapping
    public ResponseEntity<?> listDiagnostics(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String cropId,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication token is required."));
        }

        try {
            DiagnosticPageResponse response = diagnosticService.listFarmerDiagnostics(
                    token.getUid(), status, cropId, limit, cursor
            );
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            logger.warn("Access denied querying diagnostics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Bad query parameter in listDiagnostics: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (FirestoreQuotaExhaustedException e) {
            logger.error("Firestore quota exhausted listing diagnostics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error querying diagnostics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to query diagnostic records: " + e.getMessage()));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof FirebaseAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
