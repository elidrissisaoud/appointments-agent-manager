package com.dentist.rendez_vous.exception;

public class EvenementNotFoundException extends RuntimeException {
    public EvenementNotFoundException(String message) {
        super(message);
    }
}