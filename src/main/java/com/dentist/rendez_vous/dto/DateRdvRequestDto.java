package com.dentist.rendez_vous.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class DateRdvRequestDto {

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "The date must be today or in the future")
    private LocalDate date;

    @NotNull(message = "Time is required")
    private LocalTime time;
}