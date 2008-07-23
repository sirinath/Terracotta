package org.terracotta.lassen.services;

import org.terracotta.lassen.models.SignupConfirmation;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.exceptions.RegistrationException;

public interface RegistrationService {
  public String holdForEmailConfirmation(User user, String confirmationUrl) throws RegistrationException;

  public boolean confirmRegistration(SignupConfirmation confirmation) throws RegistrationException;
}