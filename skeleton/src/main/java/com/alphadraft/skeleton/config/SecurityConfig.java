package com.alphadraft.skeleton.config;

import com.alphadraft.skeleton.auth.JwtAuthenticationFilter;
import com.alphadraft.skeleton.auth.TokenService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Who may call what. This backend check is the real enforcement; the React UI hiding admin
// buttons (SCRUM-45) is only a convenience.
//   /auth/**                          anyone (sign-up and sign-in)
//   GET /stocks, GET /stocks/{id}     any signed-in user
//   POST, PUT, DELETE on /stocks      ADMIN only
//   GET /prices/**                    any signed-in user
//   POST /prices/refresh              ADMIN only
//   /league/**                        any signed-in user (joining, drafting, calls, standings)
//   /admin/**                         ADMIN only (league decisions, season, prices)
// No token or a bad token -> 401. Signed in without the needed role -> 403.
@Configuration
public class SecurityConfig {

    static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokenService) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Tokens travel in the Authorization header, not in cookies, so CSRF does not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/stocks", "/stocks/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/prices/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/prices/refresh").hasRole("ADMIN")
                        .requestMatchers("/league", "/league/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/stocks", "/stocks/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/stocks/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/stocks/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Replaces the old CorsConfig. CORS has to be handled by Spring Security now, otherwise the
    // browser's preflight request is rejected before it reaches Spring MVC.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origin:" + FRONTEND_ORIGIN + "}") String origin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}