package com.farmlink.api.service.ai;

import com.farmlink.api.dto.ai.AiDecisionContext;
import com.farmlink.api.dto.ai.AiDecisionRequest;
import com.farmlink.api.dto.ai.AiDecisionResponse;

public interface AiDecisionEngine {
    AiDecisionResponse generateDecision(AiDecisionRequest request, AiDecisionContext context);
}
