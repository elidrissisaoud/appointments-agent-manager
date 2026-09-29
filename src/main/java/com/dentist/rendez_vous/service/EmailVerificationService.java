package com.dentist.rendez_vous.service;

import com.dentist.rendez_vous.exception.TokenInvalideException;
import com.dentist.rendez_vous.model.Patient;
import com.dentist.rendez_vous.reposetory.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final PatientRepository patientRepository;
    private final EmailService emailService;

    @Value("${app.public-url}")
    private String baseUrl;

    public void creerEtEnvoyerVerification(Patient patient) {
        String token = UUID.randomUUID().toString();
        patient.setTokenVerification(token);
        patient.setEmailVerifie(false);
        patientRepository.save(patient);

        String lienConfirmation = baseUrl + "/rdv/confirmer-email?token=" + token;
        emailService.envoyerEmailVerification(
                patient.getEmail(),
                patient.getName(),
                lienConfirmation
        );
    }

    public Patient confirmerEmail(String token) {
        Patient patient = patientRepository.findByTokenVerification(token)
                .orElseThrow(() -> new TokenInvalideException(
                        "This confirmation link is invalid or has already been used."
                ));

        patient.setEmailVerifie(true);
        patient.setTokenVerification(null);
        return patientRepository.save(patient);
    }
}