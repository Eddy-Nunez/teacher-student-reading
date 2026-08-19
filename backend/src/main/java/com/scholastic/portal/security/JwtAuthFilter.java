package com.scholastic.portal.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stateless JWT auth filter. Reads the {@code Authorization: Bearer <token>} header,
 * validates it, and populates the security context. No server-side session state.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            JwtService.TokenClaims claims = jwtService.parse(header.substring(7));
            if (claims != null) {
                var principal = new AppPrincipal(claims.userId(), claims.username(), claims.role());
                var authorities = List.of(
                        new SimpleGrantedAuthority(claims.role().name()),
                        new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
            }
        }
        filterChain.doFilter(request, response);
    }
}