package com.dentist.rendez_vous.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrendreRdvRequestDto {

    @Valid
    private PatientRequestDto patient = new PatientRequestDto();

    @Valid
    private DateRdvRequestDto date = new DateRdvRequestDto();

    @Valid
    private RendezVousRequestDto rendezVous = new RendezVousRequestDto();
}