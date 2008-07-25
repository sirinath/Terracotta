package org.terracotta.lassen.services.exceptions;

public class PasswordResetConfirmationEmailException extends PasswordResetException {
  private final String email;
  private final String code;
  
  public PasswordResetConfirmationEmailException(final String email, final String code, final Throwable cause) {
    super("Unexpected error while sending the password reset confirmation email with code '" + code + "' to '" + email + "'", cause);
    this.email = email;
    this.code = code;
  }

  public String getEmail() {
    return email;
  }

  public String getCode() {
    return code;
  }
}