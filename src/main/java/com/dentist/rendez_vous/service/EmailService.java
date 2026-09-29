package com.dentist.rendez_vous.service;

import com.google.api.client.util.DateTime;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    // ==========================================================
    // VERIFICATION EMAIL (Step 1)
    // ==========================================================
    public void envoyerEmailVerification(String destinataire, String nomPatient, String lienConfirmation) {
        Context context = new Context();
        context.setVariable("nomPatient", nomPatient);
        context.setVariable("lienConfirmation", lienConfirmation);

        String htmlContent = templateEngine.process("email/verification-email", context);
        envoyerHtml(destinataire, "Confirm your email address", htmlContent);
    }

    // ==========================================================
    // APPOINTMENT CONFIRMATION EMAIL (Step 2)
    // ==========================================================
    public void envoyerEmailConfirmationRdv(String destinataire, String nomPatient,
                                            LocalDate date, LocalTime heure,
                                            String raison, String description) {

        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
        DateTimeFormatter formatterHeure = DateTimeFormatter.ofPattern("HH:mm");

        Context context = new Context();
        context.setVariable("nomPatient", nomPatient);
        context.setVariable("dateFormatee", date.format(formatterDate));
        context.setVariable("heureFormatee", heure.format(formatterHeure));
        context.setVariable("raison", raison);
        context.setVariable("description", description);

        String htmlContent = templateEngine.process("email/confirmation-rdv", context);
        envoyerHtml(destinataire, "Your appointment is confirmed ✓", htmlContent);
    }

    // ==========================================================
    // APPOINTMENT CANCELLATION EMAIL
    // ==========================================================
    public void envoyerEmailAnnulationRdv(String destinataire, String nomPatient,
                                          LocalDateTime date,
                                          String raisonRdv, String description, String raisonOfAnnulation) {

        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
        DateTimeFormatter formatterHeure = DateTimeFormatter.ofPattern("HH:mm");

        Context context = new Context();
        context.setVariable("nomPatient", nomPatient);
        context.setVariable("raisonRdv", raisonRdv);
        context.setVariable("dateFormatee", date.format(formatterDate));
        context.setVariable("heureFormatee", date.format(formatterHeure));
        context.setVariable("raisonAnnulation", raisonOfAnnulation);
        context.setVariable("description", description);

        String htmlContent = templateEngine.process("email/annulation-rdv", context);
        envoyerHtml(destinataire, "Your appointment has been cancelled", htmlContent);
    }

    // ==========================================================
    // GENERIC HTML EMAIL SENDING
    // ==========================================================
    private void envoyerHtml(String destinataire, String sujet, String contenuHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(
                    "An error occurred while sending the email to " + destinataire,
                    e
            );
        }
    }
}