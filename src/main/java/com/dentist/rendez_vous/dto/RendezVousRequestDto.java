package com.dentist.rendez_vous.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RendezVousRequestDto {

    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String raison;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}