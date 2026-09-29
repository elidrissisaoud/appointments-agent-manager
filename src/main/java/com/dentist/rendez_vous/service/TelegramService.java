package com.dentist.rendez_vous.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final WebClient telegramWebClient;

    @Value("${telegram.bot.token}")
    private String botToken;

    public Mono<Void> envoyerMessage(String chatId, String texte) {
        return telegramWebClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .bodyValue(Map.of(
                        "chat_id", chatId,
                        "text", texte,
                        "parse_mode", "Markdown"
                ))
                .retrieve()
                .bodyToMono(String.class)
                .then();
    }

    public Mono<Void> configurerWebhook(String urlPublique) {
        return telegramWebClient.get()
                .uri("/bot{token}/setWebhook?url={url}", botToken, urlPublique)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(System.out::println)
                .then();
    }
}