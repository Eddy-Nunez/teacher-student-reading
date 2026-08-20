package com.scholastic.portal.config;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.scholastic.portal.security.JwtAuthFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwtAuthFilter,
                                           @Value("${app.security.csrf-enabled:true}") boolean csrfEnabled,
                                           @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {
        http
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Apply the CORS source (deny-by-default, narrow dev allow-list) to secured endpoints.
            .cors(Customizer.withDefaults())
            // Security headers: the portal is a public, credential-bearing app, so the same-origin
            // SPA gets a strict CSP (fully self-hosted: no CDNs, no inline scripts/styles) and
            // conservative feature/referrer policies. Frame embedding is DENY (never frame this
            // app) to match the CSP frame-ancestors 'none', except when the dev H2 console (a
            // frameset UI) is actually enabled.
            .headers(h -> {
                if (h2ConsoleEnabled) {
                    h.frameOptions(f -> f.disable());
                } else {
                    h.frameOptions(f -> f.deny());
                }
                // Strict, allowlist CSP: the built SPA only loads same-origin assets and talks to
                // the same-origin /api. 'none' default hardens anything we forgot to allow.
                h.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'none'; " +
                    // React SPA + same-origin API
                    "script-src 'self'; " +
                    "script-src-elem 'self'; " +
                    "connect-src 'self'; " +
                    // Bundled Bootstrap CSS + React (no inline style attrs on pages)
                    "style-src 'self'; " +
                    "img-src 'self' data:; " +
                    "font-src 'self' data:; " +
                    // No objects, no frames-equity, no external submissions
                    "object-src 'none'; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'"));
                // Restrict browser feature/permission grants to a least-privilege set.
                h.permissionsPolicy(pp -> pp.policy(
                    "camera=(), microphone=(), geolocation=(), fullscreen=(), payment=()"));
                // Don't leak the site URL/query into cross-origin referrers.
                h.referrerPolicy(rp -> rp.policy(ReferrerPolicy.NO_REFERRER));
            })
            .authorizeHttpRequests(auth -> auth
                // Public auth + dev console (console only reachable when enabled).
                .requestMatchers("/api/auth/**", h2ConsoleEnabled ? "/h2-console/**" : "/h2-console-disabled/**", "/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Protected API namespaces (role gate for each).
                .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .requestMatchers("/api/**").authenticated()
                // Everything else is the public SPA (static assets + client-side routes → index.html).
                .anyRequest().permitAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"detail\":\"Authentication required\"}");
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"detail\":\"Access denied\"}");
                }))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (csrfEnabled) {
            // Double-submit cookie CSRF: readable XSRF token + HttpOnly auth cookie (see AuthCookie).
            http
                .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        } else {
            http.csrf(csrf -> csrf.disable());
        }

        return http.build();
    }

    /**
     * CORS is same-origin in production (SPA + /api are baked into one jar behind a single
     * origin), so cross-origin is deny-by-default: when no allowed origin is configured, no
     * {@code Access-Control-*} headers are emitted at all and foreign origins can't consume the
     * cookie/CSRF-equipped API. Only an explicit dev origin (e.g. the Vite proxy during local
     * split-frontend work) opens a narrow, allow-listed cross-origin path.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:}") String allowed) {
        var config = new CorsConfiguration();
        var origins = new java.util.ArrayList<String>();
        if (allowed != null && !allowed.isBlank()) {
            for (var o : allowed.split(",")) {
                var t = o.trim();
                if (!t.isEmpty()) origins.add(t);
            }
        }
        config.setAllowedOrigins(origins);
        // Only the methods/headers this app actually uses; never wildcard the allowed header list.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setExposedHeaders(List.of());
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * SPA-friendly CSRF request handler (Spring Security 6 documented pattern).
     * Accepts the token either via the raw request attribute (double-submit cookie) or the
     * XOR-encoded header value; writes the XSRF cookie on each request when absent.
     */
    private static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           Supplier<CsrfToken> csrfToken) {
            this.delegate.handle(request, response, csrfToken);
            csrfToken.get().getToken();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return super.resolveCsrfTokenValue(request, csrfToken);
            }
            return this.delegate.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    /** Ensures the XSRF-TOKEN cookie is written even on a request with no CSRF-relevant mutation. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}