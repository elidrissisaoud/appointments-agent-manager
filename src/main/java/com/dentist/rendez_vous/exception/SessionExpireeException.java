package com.dentist.rendez_vous.exception;

public class SessionExpireeException extends RuntimeException {
    public SessionExpireeException(String message) {
        super(message);
    }
}