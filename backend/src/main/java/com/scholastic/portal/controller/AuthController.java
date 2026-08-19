package com.scholastic.portal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.scholastic.portal.dto.LoginResponse;
import com.scholastic.portal.model.User;
import com.scholastic.portal.repository.UserRepository;
import com.scholastic.portal.security.AuthCookie;
import com.scholastic.portal.security.AppPrincipal;
import com.scholastic.portal.security.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean cookieSecure;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          @Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    public record LoginRequest(String username, String password) {}

    /**
     * Authenticates and issues the JWT in an HttpOnly, SameSite=Lax cookie. The token never
     * appears in the response body.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, AuthCookie.create(token, cookieSecure).toString())
                .body(toLoginResponse(user));
    }

    /** Validates the current session (if any) — used by the SPA on boot. */
    @GetMapping("/me")
    public LoginResponse me(@AuthenticationPrincipal AppPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalid"));
        return toLoginResponse(user);
    }

    /** Logs out by invalidating the auth cookie (clearing it client-side). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, AuthCookie.clear(cookieSecure).toString())
                .build();
    }

    /**
     * Boot-strap endpoint for the SPA so the CSRF cookie (XSRF-TOKEN) is established before the
     * first state-changing request. Returns an empty body; merely hitting it triggers the CSRF
     * filter to set the readable XSRF cookie.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrfToken() {
        return ResponseEntity.noContent().build();
    }

    private LoginResponse toLoginResponse(User user) {
        return new LoginResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}