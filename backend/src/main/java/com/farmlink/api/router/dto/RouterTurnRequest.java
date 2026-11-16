package com.farmlink.api.router.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RouterTurnRequest(
        @NotBlank(message = "sessionId must not be blank")
        String sessionId,

        @NotNull(message = "turnIndex must not be null")
        @Min(value = 1, message = "turnIndex must be >= 1")
        @Max(value = 2147483646, message = "turnIndex must be <= 2147483646")
        Long turnIndex,

        @NotBlank(message = "query must not be blank")
        @Size(max = 1000, message = "query must not exceed 1000 characters")
        String query,

        String targetOwnerUid
) {}

