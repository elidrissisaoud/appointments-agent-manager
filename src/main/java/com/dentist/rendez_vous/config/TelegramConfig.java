package com.dentist.rendez_vous.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TelegramConfig {

    @Bean
    public WebClient telegramWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }
}