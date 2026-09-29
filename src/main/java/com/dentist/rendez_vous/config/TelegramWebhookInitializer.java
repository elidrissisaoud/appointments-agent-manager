package com.dentist.rendez_vous.config;

import com.dentist.rendez_vous.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramWebhookInitializer implements ApplicationRunner {

    private final TelegramService telegramService;

    @Value("${app.public-url}")
    private String publicUrl;

    @Override
    public void run(ApplicationArguments args) {
        telegramService.configurerWebhook(publicUrl + "/webhook/telegram").subscribe();
    }
}
