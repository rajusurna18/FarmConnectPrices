package com.farmlink.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public Health API: /api/v1/health should be accessible anonymously")
    void unauthenticatedAppHealthShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("farmlink-api"));
    }

    @Test
    @DisplayName("Public Firebase Health API: /api/v1/health/firebase should be accessible anonymously")
    void unauthenticatedFirebaseHealthShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/health/firebase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("farmlink-api"));
    }

    @Test
    @DisplayName("Actuator Health: /actuator/health should be accessible anonymously for probes")
    void unauthenticatedActuatorHealthShouldReturn200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Actuator Liveness: /actuator/health/liveness should be accessible anonymously for probes")
    void unauthenticatedActuatorLivenessShouldReturn200() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Actuator Readiness: /actuator/health/readiness should be accessible anonymously for probes")
    void unauthenticatedActuatorReadinessShouldReturn200() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Actuator Info: /actuator/info must NOT be accessible anonymously (returns 401)")
    void unauthenticatedActuatorInfoShouldReturn401() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    @DisplayName("Actuator Env: /actuator/env must NOT return 2xx to anonymous requests (returns 401)")
    void unauthenticatedActuatorEnvShouldReturn401() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Actuator Metrics: /actuator/metrics must NOT return 2xx to anonymous requests (returns 401)")
    void unauthenticatedActuatorMetricsShouldReturn401() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Actuator Root: /actuator must NOT be accessible anonymously (returns 401)")
    void unauthenticatedActuatorRootShouldReturn401() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Business API Protection: /api/v1/farms must remain protected (returns 401)")
    void unauthenticatedProtectedBusinessApiShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/farms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("API Cache Privacy: /api/v1/health responses must include no-store/no-cache headers")
    void healthEndpointShouldIncludeNoStoreHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    @Test
    @DisplayName("API Cache Privacy: /api/v1/farms responses must include no-store/no-cache headers to protect private data")
    void protectedApiEndpointShouldIncludeNoStoreHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/farms"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }
}
