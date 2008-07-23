package org.terracotta.lassen.services.impl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.terracotta.lassen.models.SignupConfirmation;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.RegistrationService;
import org.terracotta.lassen.services.UserService;
import org.terracotta.lassen.services.exceptions.RegistrationConfirmationEmailException;
import org.terracotta.lassen.services.exceptions.RegistrationException;
import org.terracotta.lassen.services.exceptions.RegistrationUserStorageErrorException;

@Service
public class DefaultRegistrationService implements RegistrationService {

  private final UserService       userService;
  private final MailSender        mailSender;
  private final Map<String, Long> heldUsers = new ConcurrentHashMap<String, Long>();

  @Autowired
  public DefaultRegistrationService(final UserService userService, final MailSender mailSender) {
    this.userService = userService;
    this.mailSender = mailSender;
  }

  public String holdForEmailConfirmation(final User user, final String confirmationUrl) throws RegistrationException {
    if (null == user) { throw new IllegalArgumentException("user can't be null"); }
    if (null == confirmationUrl) { throw new IllegalArgumentException("confirmationUrl can't be null"); }

    if (!userService.store(user)) { throw new RegistrationUserStorageErrorException(user, null); }

    final UUID uuid = UUID.randomUUID();
    final String uuidString = uuid.toString().replaceAll("\\W", ""); // strip away non word chars for easy copy/paste
    heldUsers.put(uuidString, user.getId());

    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setSubject("Welcome to Examinator");
    msg.setFrom("gbevin@uwyn.com");
    msg.setTo(user.getEmail());
    msg.setText("Dear " + user.getFirstName() + " " + user.getLastName() + ",\n\n" + "thank you for registering.\n\n"
                + "Please confirm your registration by entering the code below into the form that can be found at:\n\n"
                + "Confirmation form:\n" + confirmationUrl + "\n\n" + "Confirmation code:\n" + uuidString + "\n\n"
                + "Best regards,\n\n" + "The Examinator team");
    try {
      mailSender.send(msg);
    } catch (MailException e) {
      throw new RegistrationConfirmationEmailException(user.getEmail(), uuidString, e);
    }
    return uuidString;
  }

  public boolean confirmRegistration(final SignupConfirmation confirmation) {
    if (null == confirmation) { throw new IllegalArgumentException("confirmation can't be null"); }

    if (null == confirmation.getEmail() || null == confirmation.getCode()) { return false; }

    final Long id = heldUsers.get(confirmation.getCode());
    if (null == id) {
      return false;
    } else {
      final User user = userService.findById(id);
      if (!confirmation.getEmail().equals(user.getEmail())) {
        return false;
      } else {
        user.setConfirmed(true);
        userService.store(user);
        heldUsers.remove(confirmation.getCode());
        return true;
      }
    }
  }
}