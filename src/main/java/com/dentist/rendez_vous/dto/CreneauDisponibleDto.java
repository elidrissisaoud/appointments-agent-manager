package com.dentist.rendez_vous.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class CreneauDisponibleDto {
    private LocalTime debut;
    private LocalTime fin;
}