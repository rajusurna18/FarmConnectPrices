package com.farmlink.api.orchestrator.handler;

import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.security.UserDomainRole;

/**
 * Strategy interface for executing domain-specific agricultural grounding.
 */
public interface IntentExecutionHandler {

    /**
     * Indicates whether this handler supports the given primary intent.
     */
    boolean supports(AgriIntent intent);

    /**
     * Executes read-only grounding logic against domain services.
     *
     * @param turn the upstream routing response
     * @param entities the extracted agricultural entities
     * @param callerRole the independently verified user domain role
     * @param authenticatedUid the authenticated Firebase caller UID
     * @return the grounded domain execution result
     */
    DomainExecutionResult execute(
            RouterTurnResponse turn,
            ExtractedEntitiesDto entities,
            UserDomainRole callerRole,
            String authenticatedUid
    );
}
