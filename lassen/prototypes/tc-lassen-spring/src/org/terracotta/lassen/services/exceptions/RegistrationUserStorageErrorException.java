package org.terracotta.lassen.services.exceptions;

import org.terracotta.lassen.models.User;

public class RegistrationUserStorageErrorException extends RegistrationException {
  private final User user;
  
  public RegistrationUserStorageErrorException(final User user, final Throwable cause) {
    super("Unexpected storing the user '" + user + "' while waiting for the registration finalization.", cause);
    this.user = user;
  }

  public User getUser() {
    return user;
  }
}