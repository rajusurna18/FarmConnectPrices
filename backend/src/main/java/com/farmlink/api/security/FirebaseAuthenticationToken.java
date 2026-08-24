package com.farmlink.api.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

    private final String uid;
    private final String email;
    private final String displayName;
    private final boolean emailVerified;

    public FirebaseAuthenticationToken(String uid, String email, String displayName) {
        this(uid, email, displayName, false);
    }

    public FirebaseAuthenticationToken(String uid, String email, String displayName, boolean emailVerified) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.emailVerified = emailVerified;
        setAuthenticated(true);
    }

    public FirebaseAuthenticationToken(String uid, String email, String displayName, boolean emailVerified, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.emailVerified = emailVerified;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return uid;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
