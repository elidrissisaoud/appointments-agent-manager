package com.dentist.rendez_vous.service;

import com.dentist.rendez_vous.dto.DateRdvRequestDto;
import com.dentist.rendez_vous.dto.EventsResponse;
import com.dentist.rendez_vous.dto.PatientRequestDto;
import com.dentist.rendez_vous.dto.RendezVousRequestDto;
import com.dentist.rendez_vous.exception.*;
import com.dentist.rendez_vous.mapper.PatientMapper;
import com.dentist.rendez_vous.model.Patient;
import com.dentist.rendez_vous.model.RendezVous;
import com.dentist.rendez_vous.model.StatutRendezVous;
import com.dentist.rendez_vous.reposetory.PatientRepository;
import com.dentist.rendez_vous.reposetory.RendezVousRepository;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RdvService {

    private final PatientRepository patientRepository;
    private final RendezVousRepository rendezVousRepository;
    private final PatientMapper patientMapper;
    private final GoogleCalendarService googleCalendarService;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;
    private final PatientService patientService;

    @Value("#{'${app.jours-fermes}'.split(',')}")
    private List<String> joursFermesConfig;

    private Set<DayOfWeek> joursFermes;

    private static final Map<DayOfWeek, String> NOMS_JOURS_FR = Map.of(
            DayOfWeek.MONDAY, "Monday",
            DayOfWeek.TUESDAY, "Tuesday",
            DayOfWeek.WEDNESDAY, "Wednesday",
            DayOfWeek.THURSDAY, "Thursday",
            DayOfWeek.FRIDAY, "Friday",
            DayOfWeek.SATURDAY, "Saturday",
            DayOfWeek.SUNDAY, "Sunday"
    );

    @PostConstruct
    public void init() {
        joursFermes = joursFermesConfig.stream()
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
    }

    // ==========================================================
    // STEP 1: Register the patient + send the verification email
    // ==========================================================
    @Transactional
    public Patient enregistrerPatient(PatientRequestDto patientDto) {

        Optional<Patient> patientExistant = patientRepository.findByEmail(patientDto.getEmail());

        if (patientExistant.isPresent()) {
            Patient patient = patientExistant.get();

            // Case 2: email has never been verified → resend a verification email
            if (!patient.isEmailVerifie()) {
                emailVerificationService.creerEtEnvoyerVerification(patient);
                throw new UserHaveArledyEventException(
                        "A verification link has already been sent. Please check your email inbox (or spam folder).");
            }

            // Case 3: email verified + an ongoing appointment already exists → block
            Optional<RendezVous> dernierRendezVous = rendezVousRepository
                    .findTopByPatientIdOrderByIdDesc(patient.getId());

            if (dernierRendezVous.isPresent() && dernierRendezVous.get().getStatut() == StatutRendezVous.EN_COURS) {
                throw new UserHaveArledyEventException(
                        "This account already has an ongoing appointment.");
            }

            // Case 4: email verified, no ongoing appointment → allow the patient to continue directly
            return patient;
        }

        // Case 1: new patient
        Patient nouveauPatient = patientMapper.requestToEntity(patientDto);
        nouveauPatient.setEmailVerifie(false);
        Patient patientSauvegarde = patientRepository.save(nouveauPatient);

        emailVerificationService.creerEtEnvoyerVerification(patientSauvegarde);

        return patientSauvegarde;
    }

    // ==========================================================
    // STEP 2: Finalize the appointment (after email verification)
    // ==========================================================

    public Set<DayOfWeek> getJoursFermes() {
        return joursFermes;
    }

    @Transactional
    public void finaliserRendezVous(String patientId, DateRdvRequestDto date, RendezVousRequestDto rendezVousDto) {
        Patient patient = patientRepository.findByPatientCode(patientId)
                .orElseThrow(() -> new EntityNotFoundException("No patient matches this code."));

        if (!patient.isEmailVerifie()) {
            throw new EmailNonVerifieException(
                    "Your email has not been verified yet. Please click the link you received by email.");
        }

        DayOfWeek jourChoisi = date.getDate().getDayOfWeek();
        if (joursFermes.contains(jourChoisi)) {
            throw new WeekendException(
                    "The clinic is closed on " + NOMS_JOURS_FR.get(jourChoisi) + ". Please choose another day.");
        }

        Optional<RendezVous> rendezVousAllredy = rendezVousRepository.findTopByPatientIdOrderByIdDesc(patient.getId());
        if (rendezVousAllredy.isPresent() && rendezVousAllredy.get().getStatut() == StatutRendezVous.EN_COURS) {
            throw new UserHaveEncorsEventException("This account already has an ongoing appointment.");
        }

        String eventId = googleCalendarService.creerEvenement(
                rendezVousDto.getRaison(), rendezVousDto.getDescription(), date.getDate(), date.getTime());

        RendezVous rendezVous = RendezVous.builder()
                .googleEventId(eventId)
                .patient(patient)
                .build();

        rendezVousRepository.save(rendezVous);

        emailService.envoyerEmailConfirmationRdv(
                patient.getEmail(), patient.getName(),
                date.getDate(), date.getTime(),
                rendezVousDto.getRaison(), rendezVousDto.getDescription());
    }

    public List<RendezVous> getRendezVousDuPatient(Long patientId) {
        return rendezVousRepository.findByPatientId(patientId);
    }

    public Event getDetailsEvenement(RendezVous rdv) {
        return googleCalendarService.getEvenementParId(rdv.getGoogleEventId());
    }

    private RendezVous getRendezVousByGoogleEventId(String googleEvnetId) throws RendezVousNotFoundException {
        if (!rendezVousRepository.existsByGoogleEventId(googleEvnetId)) {
            throw new RendezVousNotFoundException("This appointment does not exist.");
        }
        return rendezVousRepository.findByGoogleEventId(googleEvnetId).get();
    }

    public List<EventsResponse> getEvenementsDuJourForAiAgent(LocalDate date) {

        List<Event> events = googleCalendarService.getEvenementsDuJour(date);
        List<EventsResponse> list = new ArrayList<>();

        events.forEach(e -> {
            try {
                RendezVous rdv = getRendezVousByGoogleEventId(e.getId());
                Patient patient = patientService.getById(rdv.getPatient().getId());
                list.add(new EventsResponse(
                        e.getStart().toString(),
                        e.getEnd().toString(),
                        e.getId(),
                        e.getDescription(),
                        e.getSummary(),
                        patient.getEmail(),
                        patient.getName()));
            } catch (RendezVousNotFoundException | UserNotFoundException exception) {
                list.add(EventsResponse.builder()
                        .start(e.getStart().toString())
                        .end(e.getEnd().toString())
                        .id(e.getId())
                        .description(e.getDescription())
                        .title(e.getSummary())
                        .build());
            }
        });

        return list;
    }

    @Transactional
    public void deleteEvent(String eventId, String raison) {

        try {
            RendezVous rdv = getRendezVousByGoogleEventId(eventId);
            Patient patient = patientService.getById(rdv.getPatient().getId());
            Event event = getDetailsEvenement(rdv);
            LocalDateTime dateRdv = convertirEnLocalDateTime(event.getStart());

            googleCalendarService.supprimerEvenement(eventId);
            rdv.setStatut(StatutRendezVous.ANNULER);
            rendezVousRepository.save(rdv);

            emailService.envoyerEmailAnnulationRdv(
                    patient.getEmail(),
                    patient.getName(),
                    dateRdv,
                    event.getSummary(),
                    event.getDescription(),
                    raison);

        } catch (RendezVousNotFoundException | UserNotFoundException exception) {
            googleCalendarService.supprimerEvenement(eventId);
        }
    }

    private LocalDateTime convertirEnLocalDateTime(EventDateTime eventDateTime) {
        long millis = eventDateTime.getDateTime() != null
                ? eventDateTime.getDateTime().getValue()
                : eventDateTime.getDate().getValue();

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public List<EventsResponse> getEventsByIntervalForAiAgent(LocalDate debut, LocalDate fin) {

        List<Event> events = googleCalendarService.getEvenementsParIntervalle(debut, fin);
        List<EventsResponse> list = new ArrayList<>();

        events.forEach(e -> {
            try {
                RendezVous rdv = getRendezVousByGoogleEventId(e.getId());
                Patient patient = patientService.getById(rdv.getPatient().getId());
                list.add(new EventsResponse(
                        e.getStart().toString(),
                        e.getEnd().toString(),
                        e.getId(),
                        e.getDescription(),
                        e.getSummary(),
                        patient.getEmail(),
                        patient.getName()));
            } catch (RendezVousNotFoundException | UserNotFoundException exception) {
                list.add(EventsResponse.builder()
                        .start(e.getStart().toString())
                        .end(e.getEnd().toString())
                        .id(e.getId())
                        .description(e.getDescription())
                        .title(e.getSummary())
                        .build());
            }
        });

        return list;
    }

    public void createEventByAiAgent(String titre, String description, LocalDate date, LocalTime heure, int duree) {
        String googleId = googleCalendarService.creerEvenement(titre, description, date, heure, duree);
    }

    public List<EventsResponse> getEventsByRaisonForAiAgent(String raison) {

        List<Event> events = googleCalendarService.getEvenementsParMotif(raison);
        List<EventsResponse> list = new ArrayList<>();

        events.forEach(e -> {
            try {
                RendezVous rdv = getRendezVousByGoogleEventId(e.getId());
                Patient patient = patientService.getById(rdv.getPatient().getId());
                list.add(new EventsResponse(
                        e.getStart().toString(),
                        e.getEnd().toString(),
                        e.getId(),
                        e.getDescription(),
                        e.getSummary(),
                        patient.getEmail(),
                        patient.getName()));
            } catch (RendezVousNotFoundException | UserNotFoundException exception) {
                list.add(EventsResponse.builder()
                        .start(e.getStart().toString())
                        .end(e.getEnd().toString())
                        .id(e.getId())
                        .description(e.getDescription())
                        .title(e.getSummary())
                        .build());
            }
        });

        return list;
    }

    public String eventPasse(String eventId) {
        try {
            RendezVous rdv = getRendezVousByGoogleEventId(eventId);
            rdv.setStatut(StatutRendezVous.PASSE);
            rendezVousRepository.save(rdv);
            return "passed";

        } catch (RendezVousNotFoundException e) {
            return "This event does not correspond to any patient appointment registered in the application " +
                    "(probably an event created directly in Google Calendar by the clinic).";
        }
    }
}