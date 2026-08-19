package com.scholastic.portal.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.scholastic.portal.model.Role;

/**
 * Lightweight authenticated principal carried in the SecurityContext. The full
 * {@link com.scholastic.portal.model.User} is not loaded per-request; the JWT id/role
 * drive request authorization, and controllers dereference the user as needed.
 */
public record AppPrincipal(Long id, String username, Role role) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(role.name()),
                new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return username; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}