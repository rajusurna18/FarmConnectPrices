package com.farmlink.api.config;

import com.farmlink.api.security.FirebaseAuthFilter;
import com.google.firebase.FirebaseApp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final FirebaseApp firebaseApp;
    private final Environment environment;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Autowired
    public SecurityConfig(FirebaseApp firebaseApp, Environment environment) {
        this.firebaseApp = firebaseApp;
        this.environment = environment;
    }

    public SecurityConfig(FirebaseApp firebaseApp) {
        this(firebaseApp, null);
    }

    void setAllowedOriginsForTesting(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    private boolean isProductionEnvironment(Environment env) {
        if (env != null) {
            for (String profile : env.getActiveProfiles()) {
                if ("production".equalsIgnoreCase(profile) || "prod".equalsIgnoreCase(profile)) {
                    return true;
                }
            }
        }
        String envVar = System.getenv("SPRING_PROFILES_ACTIVE");
        return envVar != null && (envVar.toLowerCase().contains("prod") || envVar.equalsIgnoreCase("production"));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new ArrayList<>();

        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            for (String origin : allowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }

        boolean isProd = isProductionEnvironment(environment);

        if (isProd) {
            if (origins.isEmpty()) {
                String errorMsg = "CRITICAL CONFIGURATION ERROR: APP_CORS_ALLOWED_ORIGINS is missing or blank in production. " +
                        "At least one explicit trusted production origin (e.g. https://farmconnectprices.vercel.app) must be configured. " +
                        "Wildcards and open access are strictly forbidden in production. Application startup aborted.";
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            for (String origin : origins) {
                if (origin.contains("*")) {
                    String errorMsg = "CRITICAL CONFIGURATION ERROR: Wildcard origins ('" + origin + "') are strictly prohibited " +
                            "in production CORS configuration when credentials are enabled. Configure explicit trusted domain names.";
                    logger.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                String lower = origin.toLowerCase();
                if (lower.startsWith("http://localhost") || lower.startsWith("http://127.0.0.1") || lower.startsWith("https://localhost")) {
                    String errorMsg = "CRITICAL CONFIGURATION ERROR: Localhost origin ('" + origin + "') is prohibited in production " +
                            "CORS configuration. Configure the production frontend origin.";
                    logger.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
            }

            // In production, strictly set exact allowed origins
            configuration.setAllowedOrigins(origins);
        } else {
            // Non-production (development / test) profile
            if (origins.isEmpty()) {
                List<String> defaultPatterns = List.of(
                    "http://localhost:[*]",
                    "http://127.0.0.1:[*]",
                    "https://*.vercel.app"
                );
                origins.addAll(defaultPatterns);
            }
            configuration.setAllowedOriginPatterns(origins);
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!isProductionEnvironment(environment)) {
            http.addFilterBefore(new OncePerRequestFilter() {
                private static final String TEST_REPO_ATTR =
                    "org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors$SecurityContextRequestPostProcessorSupport$TestSecurityContextRepository.REPO";

                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                        throws ServletException, IOException {
                    SecurityContext current = SecurityContextHolder.getContext();
                    if (current != null && current.getAuthentication() != null
                            && current.getAuthentication().isAuthenticated()
                            && !(current.getAuthentication() instanceof AnonymousAuthenticationToken)) {
                        request.setAttribute(RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME, current);
                        request.setAttribute(TEST_REPO_ATTR, current);
                    } else {
                        SecurityContext empty = SecurityContextHolder.createEmptyContext();
                        request.setAttribute(RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME, empty);
                        request.setAttribute(TEST_REPO_ATTR, empty);
                    }
                    filterChain.doFilter(request, response);
                }
            }, SecurityContextHolderFilter.class);
        }

        http
            .securityContext(context -> context.securityContextRepository(securityContextRepository()))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"status\":\"UNAUTHORIZED\",\"message\":\"Authentication is required to access this resource\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .requestMatchers(
                    "/api/v1/health",
                    "/api/v1/health/firebase",
                    "/actuator/health",
                    "/actuator/health/liveness",
                    "/actuator/health/readiness",
                    "/error"
                ).permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/locations/**",
                    "/api/v1/crops/**",
                    "/api/v1/markets/**",
                    "/api/v1/market-prices/**",
                    "/api/v1/market-intelligence/**",
                    "/api/v1/marketplace/listings/**"
                ).permitAll()
                .requestMatchers("/api/v1/marketplace/payments/webhook/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new FirebaseAuthFilter(firebaseApp), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
