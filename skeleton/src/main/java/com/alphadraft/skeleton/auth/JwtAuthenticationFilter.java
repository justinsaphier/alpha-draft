package com.alphadraft.skeleton.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Reads "Authorization: Bearer <token>" and, if the token is valid, marks the request as
// signed in with the role from the token (as ROLE_PLAYER or ROLE_ADMIN). A missing, expired
// or tampered token leaves the request anonymous; SecurityConfig then answers 401 for any
// endpoint that needs a signed-in user.
//
// Deliberately not a @Component: Spring Boot would otherwise also register it as a plain
// servlet filter. SecurityConfig creates it and adds it to the security filter chain.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        Claims claims;
        try {
            claims = tokenService.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return; // Invalid token: stay anonymous.
        }
        String role = claims.get(TokenService.ROLE_CLAIM, String.class);
        if (claims.getSubject() == null || role == null) {
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(
                claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}