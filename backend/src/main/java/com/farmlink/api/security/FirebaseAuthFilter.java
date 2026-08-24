package com.farmlink.api.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthFilter.class);

    private final FirebaseApp firebaseApp;

    public FirebaseAuthFilter(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Allow OPTIONS preflight requests to pass through without token verification
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String idToken = authHeader.substring(7).trim();

            if (!idToken.isEmpty()) {
                try {
                    FirebaseToken decodedToken = FirebaseAuth.getInstance(firebaseApp).verifyIdToken(idToken);
                    String uid = decodedToken.getUid();
                    String email = decodedToken.getEmail();
                    String name = decodedToken.getName();
                    boolean emailVerified = decodedToken.isEmailVerified();

                    FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(uid, email, name, emailVerified);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (Exception e) {
                    logger.warn("Firebase ID token verification failed: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"status\":\"UNAUTHORIZED\",\"message\":\"Invalid or expired Firebase ID token\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
