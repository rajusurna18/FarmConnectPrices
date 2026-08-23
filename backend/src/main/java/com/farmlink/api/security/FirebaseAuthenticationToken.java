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

    public FirebaseAuthenticationToken(String uid, String email, String displayName) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        setAuthenticated(true);
    }

    public FirebaseAuthenticationToken(String uid, String email, String displayName, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
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
}
