package org.terracotta.lassen.services.exceptions;

public class PasswordResetException extends Exception {
  public PasswordResetException(final String message, final Throwable cause) {
    super(message, cause);
  }
}