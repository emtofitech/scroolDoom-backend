package com.scrolldoom.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@Profile("test")
public class LocalTestSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(LocalTestSecurityConfig.class);

    @PostConstruct
    public void warn() {
        log.warn("Test security profile is ACTIVE. Uses X-Test-UserId header for auth. NOT suitable for production.");
    }

    @Bean
    public SecurityFilterChain localTestSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("POST", "/api/v1/auth/register").permitAll()
                .requestMatchers("GET", "/api/v1/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(testJwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private Filter testJwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String testUserId = request.getHeader("X-Test-UserId");
                if (testUserId != null && !testUserId.isEmpty()) {
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            testUserId, null, null);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
