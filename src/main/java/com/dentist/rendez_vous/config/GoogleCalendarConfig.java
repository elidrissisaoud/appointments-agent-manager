package com.dentist.rendez_vous.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleCalendarConfig {

    @Value("${google.calendar.credentials.path}")
    private Resource credentialsResource;

    @Value("${google.calendar.application.name}")
    private String applicationName;

    @Bean
    public Calendar googleCalendarClient() throws IOException {
        try (InputStream credentialsStream = credentialsResource.getInputStream()) {

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(credentialsStream)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName(applicationName)
                    .build();

        } catch (GeneralSecurityException e) {
            throw new IOException("Erreur lors de l'initialisation du client Google Calendar", e);
        }
    }
}