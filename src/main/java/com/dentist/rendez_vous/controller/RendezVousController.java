package com.dentist.rendez_vous.controller;

import com.dentist.rendez_vous.dto.CreneauDisponibleDto;
import com.dentist.rendez_vous.dto.PatientRequestDto;
import com.dentist.rendez_vous.dto.RdvDetailsRequestDto;
import com.dentist.rendez_vous.exception.*;
import com.dentist.rendez_vous.model.Patient;
import com.dentist.rendez_vous.service.EmailVerificationService;
import com.dentist.rendez_vous.service.GoogleCalendarService;
import com.dentist.rendez_vous.service.RdvService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rdv")
@RequiredArgsConstructor
public class RendezVousController {

    private final RdvService service;
    private final EmailVerificationService emailVerificationService;
    private final GoogleCalendarService googleCalendarService;

    private static final String SESSION_PATIENT_ID = "patientId";

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    // ============ Step 1: Patient form ============
    @GetMapping("/patient")
    public String showPatientForm(Model model) {
        if (!model.containsAttribute("patientRequest")) {
            model.addAttribute("patientRequest", new PatientRequestDto());
        }
        return "patient-form";
    }

    @PostMapping("/patient")
    public String soumettrePatient(@Valid @ModelAttribute("patientRequest") PatientRequestDto patientRequest,
                                   BindingResult bindingResult,
                                   HttpSession session) {

        if (bindingResult.hasErrors()) {
            return "patient-form";
        }

        Patient patient;
        try {
            patient = service.enregistrerPatient(patientRequest);
        } catch (UserHaveArledyEventException ex) {
            bindingResult.rejectValue(
                    "email",
                    "email.deja.existe",
                    ex.getMessage()
            );
            return "patient-form";
        }

        // If the email has already been verified (case 4), skip directly to step 2
        if (patient.isEmailVerifie()) {
            session.setAttribute(SESSION_PATIENT_ID, patient.getPatientCode());
            return "redirect:/rdv/details";
        }

        // Otherwise (new patient or email not yet verified), redirect to the waiting page
        return "redirect:/rdv/email-envoye";
    }

    @GetMapping("/email-envoye")
    public String emailEnvoyePage() {
        return "email-envoye";
    }

    // ============ Email confirmation (link clicked) ============
    @GetMapping("/confirmer-email")
    public String confirmerEmail(@RequestParam String token, HttpSession session) {
        Patient patient = emailVerificationService.confirmerEmail(token);
        session.setAttribute(SESSION_PATIENT_ID, patient.getPatientCode());
        return "redirect:/rdv/details";
    }

    // ============ Step 2: Date + appointment form ============
    @GetMapping("/details")
    public String showDetailsForm(Model model, HttpSession session) {
        verifierSession(session);

        if (!model.containsAttribute("detailsRequest")) {
            model.addAttribute("detailsRequest", new RdvDetailsRequestDto());
        }

        model.addAttribute("joursFermes", service.getJoursFermes());

        return "rdv-details-form";
    }

    @GetMapping("/creneaux-disponibles")
    @ResponseBody
    public ResponseEntity<?> getCreneauxDisponibles(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        // Directly reject past dates without even querying Google Calendar
        if (date.isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "It is not possible to select a past date."
                    ));
        }

        try {
            List<CreneauDisponibleDto> creneaux =
                    googleCalendarService.getCreneauxLibres(date);

            if (creneaux.isEmpty()) {
                return ResponseEntity.ok()
                        .body(Map.of(
                                "message",
                                "No available time slots for this date.",
                                "creneaux",
                                creneaux
                        ));
            }

            return ResponseEntity.ok(creneaux);

        } catch (GoogleCalendarException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error",
                            "Unable to contact the calendar at the moment. Please try again later."
                    ));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error",
                            "An unexpected error occurred."
                    ));
        }
    }

    @PostMapping("/details")
    public String soumettreDetails(
            @Valid
            @ModelAttribute("detailsRequest")
            RdvDetailsRequestDto detailsRequest,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        String patientId = verifierSession(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "joursFermes",
                    service.getJoursFermes()
            );
            return "rdv-details-form";
        }

        try {
            service.finaliserRendezVous(
                    patientId,
                    detailsRequest.getDate(),
                    detailsRequest.getRendezVous()
            );

        } catch (LogicalDateTime ex) {
            bindingResult.rejectValue(
                    "date.date",
                    "date.passee",
                    ex.getMessage()
            );

            model.addAttribute(
                    "joursFermes",
                    service.getJoursFermes()
            );

            return "rdv-details-form";

        } catch (OurTimeConditionException ex) {
            bindingResult.rejectValue(
                    "date.time",
                    "heure.hors.horaires",
                    ex.getMessage()
            );

            model.addAttribute(
                    "joursFermes",
                    service.getJoursFermes()
            );

            return "rdv-details-form";

        } catch (CreneauIndisponibleException ex) {
            bindingResult.rejectValue(
                    "date.time",
                    "creneau.indisponible",
                    ex.getMessage()
            );

            model.addAttribute(
                    "joursFermes",
                    service.getJoursFermes()
            );

            return "rdv-details-form";

        } catch (WeekendException ex) {
            bindingResult.rejectValue(
                    "date.date",
                    "jour.ferme",
                    ex.getMessage()
            );

            model.addAttribute(
                    "joursFermes",
                    service.getJoursFermes()
            );

            return "rdv-details-form";
        }

        session.removeAttribute(SESSION_PATIENT_ID);

        redirectAttributes.addFlashAttribute(
                "success",
                "Your appointment has been successfully confirmed!"
        );

        return "redirect:/rdv/succes";
    }

    @GetMapping("/succes")
    public String successPage() {
        return "rendezvous-success";
    }

    private String verifierSession(HttpSession session) {
        String patientId =
                (String) session.getAttribute(SESSION_PATIENT_ID);

        if (patientId == null) {
            throw new SessionExpireeException(
                    "Your session has expired or your email has not been verified yet. Please start again."
            );
        }

        return patientId;
    }
}
