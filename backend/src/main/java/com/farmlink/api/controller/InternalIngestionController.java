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
    private final com.farmlink.api.service.AgmarknetCoverageAuditService auditService;
    private final com.farmlink.api.service.DiscoveryIndexSyncService discoveryIndexSyncService;

    @Value("${app.internal.ingest-secret:}")
    private String internalIngestSecret;

    public InternalIngestionController(DataGovIngestionService ingestionService,
                                        com.farmlink.api.service.AgmarknetCoverageAuditService auditService,
                                        com.farmlink.api.service.DiscoveryIndexSyncService discoveryIndexSyncService) {
        this.ingestionService = ingestionService;
        this.auditService = auditService;
        this.discoveryIndexSyncService = discoveryIndexSyncService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> triggerIngestion(
            @RequestHeader(value = "X-Internal-Secret", required = false) String providedSecret,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "resourceId", required = false) String resourceId,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "maxPages", required = false) Integer maxPages,
            @RequestParam(value = "lookbackDays", required = false) Integer lookbackDays,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "limit", required = false) Integer limit,
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

        Integer effectivePageSize = pageSize != null ? pageSize : limit;
        logger.info("Internal manual ingestion triggered by UID: {}, mode: {}, resourceId: {}, pageSize: {}, maxPages: {}",
                authentication.getName(), mode, resourceId, effectivePageSize, maxPages);

        IngestionResultDto result = ingestionService.ingestMandiPrices(
                mode, resourceId, effectivePageSize, maxPages, lookbackDays, offset
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/coverage-audit")
    public ResponseEntity<?> triggerCoverageAudit(
            @RequestHeader(value = "X-Internal-Secret", required = false) String providedSecret,
            @RequestParam(value = "resourceId", required = false) String resourceId,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "maxPages", required = false) Integer maxPages,
            @RequestParam(value = "offset", required = false) Integer offset,
            Authentication authentication
    ) {
        // Enforce administrative / internal authorization
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication is required to perform coverage audit.");
        }

        // If an internal secret is configured, require header match
        if (internalIngestSecret != null && !internalIngestSecret.trim().isEmpty()) {
            if (providedSecret == null || !internalIngestSecret.trim().equals(providedSecret.trim())) {
                logger.warn("Unauthorized coverage audit attempt by UID: {}", authentication.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Forbidden: Invalid internal authorization secret.");
            }
        }

        logger.info("Internal coverage audit triggered by UID: {}, resourceId: {}, pageSize: {}, maxPages: {}, offset: {}",
                authentication.getName(), resourceId, pageSize, maxPages, offset);

        com.farmlink.api.dto.CoverageAuditResultDto result = auditService.performCoverageAudit(
                resourceId, pageSize, maxPages, offset
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync-discovery-index")
    public ResponseEntity<?> syncDiscoveryIndex(
            @RequestHeader(value = "X-Internal-Secret", required = false) String providedSecret,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication is required to sync discovery index.");
        }

        if (internalIngestSecret != null && !internalIngestSecret.trim().isEmpty()) {
            if (providedSecret == null || !internalIngestSecret.trim().equals(providedSecret.trim())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Forbidden: Invalid internal authorization secret.");
            }
        }

        logger.info("Internal discovery index sync triggered by UID: {}", authentication.getName());
        var metrics = discoveryIndexSyncService.syncDiscoveryIndexFromObservedData();
        return ResponseEntity.ok(metrics);
    }
}



