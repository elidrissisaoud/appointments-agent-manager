package com.dentist.rendez_vous.reposetory;

import com.dentist.rendez_vous.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByTokenVerification(String token);
    boolean existsByEmail(String email);
    Optional<Patient> findByPatientCode(String patientCode);
}
