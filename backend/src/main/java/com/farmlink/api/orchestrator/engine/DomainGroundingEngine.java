package com.farmlink.api.orchestrator.engine;

import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.handler.IntentExecutionHandler;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.exception.UnauthorizedEconomicsAccessException;
import com.farmlink.api.router.security.UserDomainRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

/**
 * Coordinates intent handlers and bounds individual domain lookups to a strict 3,000 ms timeout.
 */
@Component
public class DomainGroundingEngine {

    private static final Logger log = LoggerFactory.getLogger(DomainGroundingEngine.class);
    public static final long DOMAIN_CALL_TIMEOUT_MS = 3000L;

    private final List<IntentExecutionHandler> handlers;
    private final Executor executor;

    public DomainGroundingEngine(
            List<IntentExecutionHandler> handlers,
            @Qualifier("orchestratorTaskExecutor") Executor executor
    ) {
        this.handlers = handlers;
        this.executor = executor;
    }

    public DomainExecutionResult ground(
            RouterTurnResponse turn,
            ExtractedEntitiesDto entities,
            UserDomainRole callerRole,
            String authenticatedUid
    ) {
        AgriIntent intent = turn.intent();
        IntentExecutionHandler selectedHandler = null;

        for (IntentExecutionHandler handler : handlers) {
            if (handler.supports(intent)) {
                selectedHandler = handler;
                break;
            }
        }

        if (selectedHandler == null) {
            log.warn("No handler found supporting intent: {}", intent);
            return DomainExecutionResult.failure(
                    intent,
                    List.of("No execution handler registered for intent " + intent),
                    "No execution handler registered for intent " + intent,
                    0L
            );
        }

        final IntentExecutionHandler handlerToRun = selectedHandler;
        long startTime = System.currentTimeMillis();
        CompletableFuture<DomainExecutionResult> future = null;

        try {
            future = CompletableFuture.supplyAsync(
                    () -> handlerToRun.execute(turn, entities, callerRole, authenticatedUid),
                    executor
            );
            return future.get(DOMAIN_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ree) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("Domain grounding task rejected by executor (queue saturated) for intent '{}': {}", intent, ree.getMessage());
            return DomainExecutionResult.failure(
                    intent,
                    List.of("Domain grounding executor saturated: " + ree.getMessage()),
                    "Domain service busy. Task rejected by executor.",
                    latency
            );
        } catch (TimeoutException te) {
            if (future != null) {
                future.cancel(true);
            }
            long latency = System.currentTimeMillis() - startTime;
            log.warn("Domain lookup timed out after {} ms for intent: {}", latency, intent);
            return DomainExecutionResult.failure(
                    intent,
                    List.of("Domain grounding timed out after " + DOMAIN_CALL_TIMEOUT_MS + " ms"),
                    "Domain service did not respond within the allocated timeout window.",
                    latency
            );
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
            if (cause instanceof UnauthorizedEconomicsAccessException ueae) {
                throw ueae;
            }
            if (cause instanceof AccessDeniedException ade) {
                throw ade;
            }
            long latency = System.currentTimeMillis() - startTime;
            log.error("Domain lookup error for intent {}: {}", intent, cause.getMessage(), cause);
            return DomainExecutionResult.failure(
                    intent,
                    List.of("Domain lookup failed: " + cause.getMessage()),
                    "Domain service encountered an unexpected error.",
                    latency
            );
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            if (future != null) {
                future.cancel(true);
            }
            long latency = System.currentTimeMillis() - startTime;
            return DomainExecutionResult.failure(
                    intent,
                    List.of("Domain grounding thread interrupted"),
                    "Operation interrupted.",
                    latency
            );
        }
    }
}
