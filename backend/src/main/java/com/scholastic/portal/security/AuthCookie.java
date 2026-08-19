package com.scholastic.portal.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;

/**
 * Central definition of the authentication cookie.
 *
 * Security properties:
 * - {@code HttpOnly}: not readable by document.cookie → an XSS payload cannot exfiltrate the token.
 * - {@code SameSite=Lax}: cookie is not sent on cross-site requests → blocks CSRF from other origins.
 * - {@code Secure}: only sent over HTTPS. Enabled in production via the {@code app.cookie.secure}
 *   property (off by default so local http://localhost dev keeps working; localhost is a
 *   trustworthy origin in browsers, but we still only enable it explicitly).
 *
 * CSRF: the SPA uses a double-submit cookie pattern (see SecurityConfig) — a readable XSRF cookie
 * for the header, while THIS auth cookie stays httpOnly.
 */
public final class AuthCookie {

    public static final String NAME = "portal_token";
    public static final Duration MAX_AGE = Duration.ofHours(24);

    private AuthCookie() {
    }

    public static ResponseCookie create(String token, boolean secure) {
        return ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(MAX_AGE)
                .build();
    }

    public static ResponseCookie clear(boolean secure) {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}