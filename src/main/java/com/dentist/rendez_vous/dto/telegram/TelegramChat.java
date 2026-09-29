package com.dentist.rendez_vous.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramChat {
    private Long id;
    private String firstName;
    private String username;
}