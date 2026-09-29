package com.dentist.rendez_vous.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramMessage {
    private Long messageId;
    private TelegramChat chat;
    private String text;
}