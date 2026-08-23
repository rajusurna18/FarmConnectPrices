package com.farmlink.api.config;

import com.farmlink.api.security.FirebaseAuthFilter;
import com.google.firebase.FirebaseApp;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseApp firebaseApp;

    public SecurityConfig(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                .requestMatchers("/api/v1/health", "/api/v1/health/firebase", "/actuator/**").permitAll()
                .requestMatchers("/api/v1/users/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(new FirebaseAuthFilter(firebaseApp), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
