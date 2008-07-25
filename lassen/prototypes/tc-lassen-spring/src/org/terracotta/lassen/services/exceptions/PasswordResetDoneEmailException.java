package org.terracotta.lassen.services.exceptions;

public class PasswordResetDoneEmailException extends PasswordResetException {
  private final String email;
  
  public PasswordResetDoneEmailException(final String email, final Throwable cause) {
    super("Unexpected error while sending the newly generated to '" + email + "'", cause);
    this.email = email;
  }

  public String getEmail() {
    return email;
  }
}