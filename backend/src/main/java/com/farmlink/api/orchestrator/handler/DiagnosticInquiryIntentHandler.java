package com.farmlink.api.orchestrator.handler;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.DiagnosticPageResponse;
import com.farmlink.api.dto.DiagnosticRecordResponse;
import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.engine.EntityResolverService;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.security.UserDomainRole;
import com.farmlink.api.service.CropDiagnosticService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Handles PEST_MANAGEMENT and PLANT_DISEASE_INQUIRY with strict tenant privacy.
 * Diagnostic records are queried ONLY if caller is a verified FARMER for their OWN records.
 * Non-farmers (MEDIATOR_BUYER, CUSTOMER) receive general IPM/disease guidelines with ZERO diagnostic lookups.
 */
@Component
public class DiagnosticInquiryIntentHandler implements IntentExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticInquiryIntentHandler.class);

    private final EntityResolverService entityResolver;
    private final CropDiagnosticService cropDiagnosticService;

    public DiagnosticInquiryIntentHandler(
            EntityResolverService entityResolver,
            @Autowired(required = false) CropDiagnosticService cropDiagnosticService
    ) {
        this.entityResolver = entityResolver;
        this.cropDiagnosticService = cropDiagnosticService;
    }

    @Override
    public boolean supports(AgriIntent intent) {
        return intent == AgriIntent.PEST_MANAGEMENT || intent == AgriIntent.PLANT_DISEASE_INQUIRY;
    }

    @Override
    public DomainExecutionResult execute(
            RouterTurnResponse turn,
            ExtractedEntitiesDto entities,
            UserDomainRole callerRole,
            String authenticatedUid
    ) {
        long start = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        Map<String, Object> data = new LinkedHashMap<>();

        String rawCrop = !entities.crops().isEmpty() ? entities.crops().get(0) : null;
        Optional<CropResponse> cropOpt = entityResolver.resolveCrop(rawCrop);
        String cropId = cropOpt.map(CropResponse::getId).orElse(null);
        String cropName = cropOpt.map(CropResponse::getName).orElse(rawCrop != null ? rawCrop : "Crop");

        data.put("cropName", cropName);
        data.put("scientificName", cropOpt.map(CropResponse::getScientificName).orElse(""));
        data.put("inquiryType", turn.intent().name());

        // Privacy Gate: Only query Diagnostic history if caller is verified FARMER
        if (callerRole == UserDomainRole.FARMER && cropDiagnosticService != null && authenticatedUid != null) {
            try {
                DiagnosticPageResponse diagPage = cropDiagnosticService.listFarmerDiagnostics(
                        authenticatedUid,
                        "ALL",
                        cropId,
                        5,
                        null
                );
                List<DiagnosticRecordResponse> items = diagPage != null ? diagPage.getItems() : Collections.emptyList();
                if (items != null && !items.isEmpty()) {
                    List<Map<String, Object>> userHistory = new ArrayList<>();
                    for (DiagnosticRecordResponse d : items) {
                        Map<String, Object> rec = new LinkedHashMap<>();
                        rec.put("diagnosticId", d.getId());
                        rec.put("cropName", d.getCropName());
                        rec.put("symptoms", d.getSymptoms());
                        rec.put("affectedPart", d.getAffectedPart());
                        rec.put("status", d.getStatus());
                        rec.put("createdAt", d.getCreatedAt());
                        userHistory.add(rec);
                    }
                    data.put("farmerPriorDiagnostics", userHistory);
                    data.put("hasPriorScanHistory", true);
                } else {
                    data.put("hasPriorScanHistory", false);
                }
            } catch (Exception e) {
                log.debug("Diagnostic history lookup skipped or unavailable for farmer: {}", e.getMessage());
                data.put("hasPriorScanHistory", false);
            }
        } else {
            // Non-farmers (BUYER, CUSTOMER) receive public agronomic advice with ZERO private records inspected
            data.put("hasPriorScanHistory", false);
        }

        data.put("advisoryNote",
                "For automated diagnostic identification and disease detection, please upload a clear photo of the affected plant leaf using FarmLink Vision Diagnostic upload.");
        data.put("disclaimer", AgronomicIntentHandler.GENERAL_EXTENSION_DISCLAIMER);

        String summary = String.format("Grounded %s advisory for %s. Recommend uploading leaf photo for automated visual diagnosis. %s",
                turn.intent() == AgriIntent.PEST_MANAGEMENT ? "pest management" : "plant disease",
                cropName,
                AgronomicIntentHandler.GENERAL_EXTENSION_DISCLAIMER);

        long latency = System.currentTimeMillis() - start;
        return DomainExecutionResult.success(turn.intent(), data, summary, latency);
    }
}
