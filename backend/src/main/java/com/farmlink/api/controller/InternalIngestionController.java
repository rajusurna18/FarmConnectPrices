package com.farmlink.api.controller;

import com.farmlink.api.dto.IngestionResultDto;
import com.farmlink.api.service.DataGovIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/market-prices")
public class InternalIngestionController {

    private static final Logger logger = LoggerFactory.getLogger(InternalIngestionController.class);

    private final DataGovIngestionService ingestionService;

    @Value("${app.internal.ingest-secret:}")
    private String internalIngestSecret;

    public InternalIngestionController(DataGovIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> triggerIngestion(
            @RequestHeader(value = "X-Internal-Secret", required = false) String providedSecret,
            @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
            Authentication authentication
    ) {
        // Enforce administrative / internal authorization
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication is required to trigger ingestion.");
        }

        // If an internal secret is configured, require header match
        if (internalIngestSecret != null && !internalIngestSecret.trim().isEmpty()) {
            if (providedSecret == null || !internalIngestSecret.trim().equals(providedSecret.trim())) {
                logger.warn("Unauthorized ingestion trigger attempt by UID: {}", authentication.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Forbidden: Invalid internal authorization secret.");
            }
        }

        logger.info("Internal manual ingestion triggered by UID: {}, limit: {}", authentication.getName(), limit);
        IngestionResultDto result = ingestionService.ingestMandiPrices(limit);
        return ResponseEntity.ok(result);
    }
}
