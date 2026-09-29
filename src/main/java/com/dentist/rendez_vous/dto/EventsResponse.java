package com.dentist.rendez_vous.dto;

import com.google.api.client.util.DateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class EventsResponse {
    private String start;
    private String end;
    private String id;
    private String description;
    private String title;
    private String email;
    private String name;
}
