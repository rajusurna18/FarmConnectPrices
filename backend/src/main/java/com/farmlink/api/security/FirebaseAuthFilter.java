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

    private static final Logger logger =
            LoggerFactory.getLogger(FirebaseAuthFilter.class);

    private final FirebaseApp firebaseApp;

    public FirebaseAuthFilter(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // Requests without a token continue. Spring Security decides
        // whether the requested endpoint requires authentication.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7).trim();

        if (idToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            FirebaseToken decodedToken =
                    FirebaseAuth.getInstance(firebaseApp).verifyIdToken(idToken);

            FirebaseAuthenticationToken authentication =
                    new FirebaseAuthenticationToken(
                            decodedToken.getUid(),
                            decodedToken.getEmail(),
                            decodedToken.getName(),
                            decodedToken.isEmailVerified()
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (Exception e) {
            logger.warn("Firebase ID token verification failed: {}",
                    e.getMessage());

            SecurityContextHolder.clearContext();

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":\"UNAUTHORIZED\","
                    + "\"message\":\"Invalid or expired Firebase ID token\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
