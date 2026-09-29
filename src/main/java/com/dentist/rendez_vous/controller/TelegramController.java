package com.dentist.rendez_vous.controller;

import com.dentist.rendez_vous.agent.AiAgent;
import com.dentist.rendez_vous.dto.telegram.TelegramUpdate;
import com.dentist.rendez_vous.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class TelegramController {

    private final AiAgent aiAgent;
    private final TelegramService telegramService;

    @Value("${telegram.admin.chat-id}")
    private String adminChatId;

    @PostMapping("/telegram")
    public Mono<Void> recevoirMessage(@RequestBody TelegramUpdate update) {

        if (update.getMessage() == null || update.getMessage().getText() == null) {
            return Mono.empty();
        }

        String chatId = String.valueOf(update.getMessage().getChat().getId());
        String texteRecu = update.getMessage().getText();

        //  only chatId authorized to connect the bot
        if (!chatId.equals(adminChatId)) {
            log.warn("not authorized account : {}", chatId);
            return telegramService.envoyerMessage(chatId,
                    "sorry you can't use this bot.");
        }

        return aiAgent.askAgent(texteRecu, chatId)
                .collectList()
                .map(chunks -> String.join("", chunks))
                .flatMap(reponse -> telegramService.envoyerMessage(chatId, reponse));
    }
}