package com.dentist.rendez_vous.exception;

public class EmailNonVerifieException extends RuntimeException {
  public EmailNonVerifieException(String message) {
    super(message);
  }
}