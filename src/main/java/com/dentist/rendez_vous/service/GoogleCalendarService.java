package com.dentist.rendez_vous.service;

import com.dentist.rendez_vous.dto.CreneauDisponibleDto;
import com.dentist.rendez_vous.dto.EventsResponse;
import com.dentist.rendez_vous.exception.*;
import com.dentist.rendez_vous.model.Patient;
import com.dentist.rendez_vous.model.RendezVous;
import com.dentist.rendez_vous.reposetory.PatientRepository;
import com.dentist.rendez_vous.reposetory.RendezVousRepository;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPattern;

import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final Calendar calendarClient;

    @Value("${google.calendar.calendar-id}")
    private String calendarId;

    @Value("${app.begin.hour}")
    private int openingHour;
    @Value("${app.begin.minute}")
    private int openingMinute;
    @Value("${app.end.hour}")
    private int closingHour;
    @Value("${app.end.minute}")
    private int closingMinute;
    @Value("${app.duration.minutes}")
    private int appointmentDurationMinutes;

    private LocalTime OPENING_TIME;
    private LocalTime CLOSING_TIME;
    private int APPOINTMENT_DURATION_MINUTES;

    @PostConstruct
    public void init() {
        OPENING_TIME = LocalTime.of(openingHour, openingMinute);
        CLOSING_TIME = LocalTime.of(closingHour, closingMinute);
        APPOINTMENT_DURATION_MINUTES = appointmentDurationMinutes;
    }

    public String creerEvenement(String titre, String description, LocalDate date, LocalTime heure) {

        LocalDateTime debut = LocalDateTime.of(date, heure);
        LocalDateTime fin = debut.plusMinutes(APPOINTMENT_DURATION_MINUTES);

        // 1. FIRST, check the opening hours (absolute rule)
        if (!ourConditionTime(debut.toLocalTime(), fin.toLocalTime())) {
            throw new OurTimeConditionException(
                    "Our office opens at " + OPENING_TIME + " and closes at " + CLOSING_TIME);
        }

        // 2. Then check that the appointment is not in the past (relative to the current time)
        if (!logicalTime(date, heure)) {
            throw new LogicalDateTime("You cannot book an appointment in the past");
        }

        // 3. Finally, check the availability of the time slot
        if (!estCreneauLibre(debut, fin)) {
            throw new CreneauIndisponibleException(
                    "The time slot on " + date + " at " + heure + " is already booked.");
        }

        Event event = new Event()
                .setSummary(titre)
                .setDescription(description);

        event.setStart(toEventDateTime(debut));
        event.setEnd(toEventDateTime(fin));

        try {
            Event createdEvent = calendarClient.events()
                    .insert(calendarId, event)
                    .execute();
            return createdEvent.getId();
        } catch (IOException e) {
            throw new GoogleCalendarException("An error occurred while creating the Google Calendar event", e);
        }
    }

    public String creerEvenement(String titre, String description, LocalDate date, LocalTime heure, int duree) {

        LocalDateTime debut = LocalDateTime.of(date, heure);
        LocalDateTime fin = debut.plusMinutes(duree);

        // 1. FIRST, check the opening hours (absolute rule)
        if (!ourConditionTime(debut.toLocalTime(), fin.toLocalTime())) {
            throw new OurTimeConditionException(
                    "Our office opens at " + OPENING_TIME + " and closes at " + CLOSING_TIME);
        }

        // 2. Then check that the appointment is not in the past (relative to the current time)
        if (!logicalTime(date, heure)) {
            throw new LogicalDateTime("You cannot book an appointment in the past");
        }

        // 3. Finally, check the availability of the time slot
        if (!estCreneauLibre(debut, fin)) {
            throw new CreneauIndisponibleException(
                    "The time slot on " + date + " at " + heure + " is already booked.");
        }

        Event event = new Event()
                .setSummary(titre)
                .setDescription(description);

        event.setStart(toEventDateTime(debut));
        event.setEnd(toEventDateTime(fin));

        try {
            Event createdEvent = calendarClient.events()
                    .insert(calendarId, event)
                    .execute();
            return createdEvent.getId();
        } catch (IOException e) {
            throw new GoogleCalendarException("An error occurred while creating the Google Calendar event", e);
        }
    }

    // ==========================================================
    // GET EVENT DETAILS BY ID
    // ==========================================================
    public Event getEvenementParId(String eventId) {
        try {
            return calendarClient.events().get(calendarId, eventId).execute();
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                throw new EvenementNotFoundException("Event not found: " + eventId);
            }
            throw new GoogleCalendarException("An error occurred while retrieving the event", e);
        }
    }

    // ==========================================================
    // DELETE AN EVENT
    // ==========================================================
    public void supprimerEvenement(String eventId) {
        try {
            calendarClient.events()
                    .delete(calendarId, eventId)
                    .execute();
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                throw new EvenementNotFoundException("Event not found: " + eventId);
            }
            throw new GoogleCalendarException("An error occurred while deleting the event", e);
        }
    }

    // ==========================================================
    // VIEW EVENTS FOR A DAY (the day's tasks/appointments)
    // ==========================================================
    public List<Event> getEvenementsDuJour(LocalDate date) {
        try {
            DateTime debutJournee = toGoogleDateTime(date.atTime(openingHour, openingMinute));
            DateTime finJournee = toGoogleDateTime(date.atTime(closingHour, closingMinute));

            Events events = calendarClient.events().list(calendarId)
                    .setTimeMin(debutJournee)
                    .setTimeMax(finJournee)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            return events.getItems();

        } catch (IOException e) {
            throw new GoogleCalendarException("An error occurred while retrieving the events", e);
        }
    }

    // ==========================================================
    // VIEW AVAILABLE TIME SLOTS FOR A DAY
    // ==========================================================
    public List<CreneauDisponibleDto> getCreneauxLibres(LocalDate date) {

        List<Event> evenementsExistants = getEvenementsDuJour(date);
        List<CreneauDisponibleDto> creneauxLibres = new ArrayList<>();

        LocalTime curseur = OPENING_TIME;

        while (curseur.plusMinutes(APPOINTMENT_DURATION_MINUTES).compareTo(CLOSING_TIME) <= 0) {
            LocalDateTime debutSlot = LocalDateTime.of(date, curseur);
            LocalDateTime finSlot = debutSlot.plusMinutes(APPOINTMENT_DURATION_MINUTES);

            boolean occupe = evenementsExistants.stream().anyMatch(event ->
                    chevauche(debutSlot, finSlot, toLocalDateTime(event.getStart()), toLocalDateTime(event.getEnd()))
            );

            if (!occupe) {
                creneauxLibres.add(
                        new CreneauDisponibleDto(
                                curseur,
                                curseur.plusMinutes(APPOINTMENT_DURATION_MINUTES)
                        )
                );
            }

            curseur = curseur.plusMinutes(APPOINTMENT_DURATION_MINUTES);
        }

        return creneauxLibres;
    }

    // ==========================================================
    // PRIVATE UTILITY METHODS
    // ==========================================================

    private boolean estCreneauLibre(LocalDateTime debut, LocalDateTime fin) {
        return estCreneauLibre(debut, fin, null);
    }

    private boolean estCreneauLibre(LocalDateTime debut, LocalDateTime fin, String eventIdAExclure) {
        List<Event> evenements = getEvenementsDuJour(debut.toLocalDate());

        return evenements.stream()
                .filter(event -> eventIdAExclure == null || !event.getId().equals(eventIdAExclure))
                .noneMatch(event -> chevauche(
                        debut,
                        fin,
                        toLocalDateTime(event.getStart()),
                        toLocalDateTime(event.getEnd())
                ));
    }

    // ==========================================================
    // SEARCH BY DATE RANGE
    // ==========================================================

    public List<Event> getEvenementsParIntervalle(LocalDate dateDebut, LocalDate dateFin) {

        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "The end date (" + dateFin + ") cannot be earlier than the start date (" + dateDebut + ")");
        }

        try {
            DateTime debutPeriode = toGoogleDateTime(dateDebut.atTime(0, 0));
            DateTime finPeriode = toGoogleDateTime(dateFin.atTime(23, 59));

            Events events = calendarClient.events().list(calendarId)
                    .setTimeMin(debutPeriode)
                    .setTimeMax(finPeriode)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> resultats = events.getItems();

            if (resultats.isEmpty()) {
                throw new AucunEvenementTrouveException(
                        "No appointments found between " + dateDebut + " and " + dateFin);
            }

            return resultats;

        } catch (IOException e) {
            throw new GoogleCalendarException(
                    "An error occurred while retrieving events for the specified date range",
                    e
            );
        }
    }

    // ==========================================================
    // SEARCH BY PATTERN / KEYWORD (title, description...)
    // ==========================================================
    public List<Event> getEvenementsParMotif(String motif) {

        try {
            Events events = calendarClient.events().list(calendarId)
                    .setQ(motif)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> resultats = events.getItems();

            if (resultats.isEmpty()) {
                throw new AucunEvenementTrouveException(
                        "No appointments found matching the pattern: \"" + motif + "\"");
            }

            return resultats;

        } catch (IOException e) {
            throw new GoogleCalendarException(
                    "An error occurred while searching by pattern",
                    e
            );
        }
    }

    private boolean chevauche(
            LocalDateTime debut1,
            LocalDateTime fin1,
            LocalDateTime debut2,
            LocalDateTime fin2
    ) {
        return debut1.isBefore(fin2) && debut2.isBefore(fin1);
    }

    private EventDateTime toEventDateTime(LocalDateTime localDateTime) {
        return new EventDateTime()
                .setDateTime(toGoogleDateTime(localDateTime))
                .setTimeZone(ZoneId.systemDefault().getId());
    }

    private DateTime toGoogleDateTime(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return new DateTime(instant.toEpochMilli());
    }

    private LocalDateTime toLocalDateTime(EventDateTime eventDateTime) {
        long millis = eventDateTime.getDateTime() != null
                ? eventDateTime.getDateTime().getValue()
                : eventDateTime.getDate().getValue();

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private boolean logicalTime(LocalDate date, LocalTime time) {
        if (date.equals(LocalDate.now())) {
            return LocalTime.now().isBefore(time);
        }

        if (date.isBefore(LocalDate.now())) {
            return false;
        }

        return date.isAfter(LocalDate.now());
    }

    private Boolean ourConditionTime(LocalTime debut, LocalTime fin) {
        return !(debut.isBefore(OPENING_TIME) || fin.isAfter(CLOSING_TIME));
    }
}