package com.farmlink.api.service;

import com.farmlink.api.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealthStatus() {
        return new HealthResponse("UP", "farmlink-api");
    }
}
