package com.farmlink.api.config;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigCorsTest {

    private FirebaseApp firebaseApp;

    @BeforeEach
    void setUp() {
        firebaseApp = Mockito.mock(FirebaseApp.class);
    }

    private CorsConfiguration extractConfiguration(CorsConfigurationSource source) {
        assertTrue(source instanceof UrlBasedCorsConfigurationSource);
        UrlBasedCorsConfigurationSource urlSource = (UrlBasedCorsConfigurationSource) source;
        return urlSource.getCorsConfigurations().get("/**");
    }

    @Test
    void devProfile_withNoConfiguredOrigins_allowsDefaultLocalAndVercelPatterns() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        SecurityConfig config = new SecurityConfig(firebaseApp, env);
        config.setAllowedOriginsForTesting("");

        CorsConfiguration cors = extractConfiguration(config.corsConfigurationSource());

        assertNotNull(cors);
        assertTrue(cors.getAllowCredentials());
        List<String> patterns = cors.getAllowedOriginPatterns();
        assertNotNull(patterns);
        assertTrue(patterns.contains("http://localhost:[*]"));
        assertTrue(patterns.contains("http://127.0.0.1:[*]"));
        assertTrue(patterns.contains("https://*.vercel.app"));
    }

    @Test
    void devProfile_withCustomOrigins_appliesConfiguredPatterns() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        SecurityConfig config = new SecurityConfig(firebaseApp, env);
        config.setAllowedOriginsForTesting("http://localhost:5173, https://custom.dev.local");

        CorsConfiguration cors = extractConfiguration(config.corsConfigurationSource());

        assertNotNull(cors);
        List<String> patterns = cors.getAllowedOriginPatterns();
        assertNotNull(patterns);
        assertEquals(2, patterns.size());
        assertTrue(patterns.contains("http://localhost:5173"));
        assertTrue(patterns.contains("https://custom.dev.local"));
    }

    @Test
    void prodProfile_withValidExplicitOrigins_configuresExactOriginsAndDoesNotIncludeLocalhostOrWildcards() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        SecurityConfig config = new SecurityConfig(firebaseApp, env);
        config.setAllowedOriginsForTesting("https://farmconnectprices.vercel.app, https://farmconnectprices.com");

        CorsConfiguration cors = extractConfiguration(config.corsConfigurationSource());

        assertNotNull(cors);
        assertTrue(cors.getAllowCredentials());
        List<String> allowedOrigins = cors.getAllowedOrigins();
        assertNotNull(allowedOrigins);
        assertEquals(2, allowedOrigins.size());
        assertTrue(allowedOrigins.contains("https://farmconnectprices.vercel.app"));
        assertTrue(allowedOrigins.contains("https://farmconnectprices.com"));

        // Must not contain any localhost or wildcard patterns
        assertNull(cors.getAllowedOriginPatterns());
        assertFalse(allowedOrigins.contains("http://localhost:5173"));
        assertFalse(allowedOrigins.contains("http://localhost:[*]"));
        assertFalse(allowedOrigins.contains("https://*.vercel.app"));
    }

    @Test
    void prodProfile_withMissingOrBlankOrigins_failsStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        SecurityConfig config = new SecurityConfig(firebaseApp, env);
        config.setAllowedOriginsForTesting("");

        IllegalStateException ex = assertThrows(IllegalStateException.class, config::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("APP_CORS_ALLOWED_ORIGINS is missing or blank in production"));

        config.setAllowedOriginsForTesting("   ,   ");
        ex = assertThrows(IllegalStateException.class, config::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("APP_CORS_ALLOWED_ORIGINS is missing or blank in production"));
    }

    @Test
    void prodProfile_withLocalhostOrigin_failsStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        SecurityConfig config = new SecurityConfig(firebaseApp, env);
        config.setAllowedOriginsForTesting("http://localhost:5173");

        IllegalStateException ex = assertThrows(IllegalStateException.class, config::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("Localhost origin"));
    }

    @Test
    void prodProfile_withWildcardPattern_failsStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        SecurityConfig config = new SecurityConfig(firebaseApp, env);
        config.setAllowedOriginsForTesting("https://*.vercel.app");

        IllegalStateException ex = assertThrows(IllegalStateException.class, config::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("Wildcard origins"));

        config.setAllowedOriginsForTesting("*");
        ex = assertThrows(IllegalStateException.class, config::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("Wildcard origins"));
    }
}

