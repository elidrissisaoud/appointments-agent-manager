package com.dentist.rendez_vous.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webhook/**").permitAll()
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/webhook/**")  // no CSRF for external  webhooks
                );

        return http.build();
    }
}