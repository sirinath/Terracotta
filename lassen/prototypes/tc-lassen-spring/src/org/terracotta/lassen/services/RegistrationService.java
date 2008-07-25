package org.terracotta.lassen.services;

import org.terracotta.lassen.models.UserCodeConfirmation;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.exceptions.RegistrationException;

public interface RegistrationService {
  public boolean holdForEmailConfirmation(User user, String confirmationUrl) throws RegistrationException;

  public boolean confirmRegistration(UserCodeConfirmation confirmation) throws RegistrationException;
}