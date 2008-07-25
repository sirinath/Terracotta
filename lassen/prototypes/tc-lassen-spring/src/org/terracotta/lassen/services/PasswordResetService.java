package org.terracotta.lassen.services;

import org.terracotta.lassen.services.exceptions.PasswordResetException;


public interface PasswordResetService {
  public boolean requestConfirmation(String email, String confirmationUrl) throws PasswordResetException;

  public boolean generateNewPassword(String code) throws PasswordResetException;
}