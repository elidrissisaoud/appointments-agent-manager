package com.dentist.rendez_vous.reposetory;

import com.dentist.rendez_vous.model.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {
    List<RendezVous> findByPatientId(Long patientId);
    Optional<RendezVous> findByGoogleEventId(String googleEventId);
    boolean existsByGoogleEventId(String GoogleEventId);
    Optional<RendezVous> findTopByPatientIdOrderByIdDesc(Long patientId);
}