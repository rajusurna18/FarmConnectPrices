package com.farmlink.api.config;

import com.farmlink.api.security.FirebaseAuthFilter;
import com.google.firebase.FirebaseApp;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseApp firebaseApp;
    private final String allowedOrigins;

    public SecurityConfig(
            FirebaseApp firebaseApp,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins
    ) {
        this.firebaseApp = firebaseApp;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                .requestMatchers("/api/v1/health", "/api/v1/health/firebase", "/actuator/**").permitAll()
                .requestMatchers("/api/v1/users/**", "/api/v1/profile/**", "/api/v1/farms/**", "/api/v1/crops/**", "/api/v1/locations/**", "/api/v1/markets/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(new FirebaseAuthFilter(firebaseApp), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
