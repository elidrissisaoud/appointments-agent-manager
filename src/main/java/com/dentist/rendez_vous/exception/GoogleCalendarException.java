package com.dentist.rendez_vous.exception;

public class GoogleCalendarException extends RuntimeException {
    public GoogleCalendarException(String message, Throwable cause) {
        super(message, cause);
    }
}