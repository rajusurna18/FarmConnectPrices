package com.farmlink.api.router.dto;

public record SessionInitResponse(
        String sessionId,
        String status,
        long ttlSeconds
) {}

