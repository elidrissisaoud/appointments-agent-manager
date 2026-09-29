package com.dentist.rendez_vous.mapper;

import com.dentist.rendez_vous.dto.PatientRequestDto;
import com.dentist.rendez_vous.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {
    public Patient requestToEntity(PatientRequestDto request){
        return Patient.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .build();
    }
}
