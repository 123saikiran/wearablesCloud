package com.example.oidcdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every request except the public landing page is gated behind OIDC login.
 * The landing page itself is public so it can render the "Sign in with..."
 * buttons and, for an authenticated visitor, the claims from their ID token.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/css/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2.loginPage("/"))
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll());

        return http.build();
    }
}
