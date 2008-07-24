package org.terracotta.lassen.services.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.terracotta.lassen.models.SignupConfirmation;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.RegistrationService;
import org.terracotta.lassen.services.UserService;
import org.terracotta.lassen.services.exceptions.RegistrationConfirmationEmailException;
import org.terracotta.lassen.services.exceptions.RegistrationException;
import org.terracotta.lassen.services.exceptions.RegistrationUserStorageErrorException;

import freemarker.template.Configuration;

@Service
public class DefaultRegistrationService implements RegistrationService {

  private final Configuration     freemarkerConfiguration;
  private final UserService       userService;
  private final MailSender        mailSender;
  private final Map<String, Long> heldUsers = new ConcurrentHashMap<String, Long>();

  private String  signupConfirmationTemplateName;
  private String  signupConfirmationSubject;
  private String  signupConfirmationFromEmail;

  @Autowired
  public DefaultRegistrationService(final Configuration freemarkerConfiguration, final UserService userService, final MailSender mailSender) {
    this.freemarkerConfiguration = freemarkerConfiguration;
    this.userService = userService;
    this.mailSender = mailSender;
  }

  public void setSignupConfirmationTemplateName(final String signupConfirmationTemplateName) {
    this.signupConfirmationTemplateName = signupConfirmationTemplateName;
  }

  public void setSignupConfirmationSubject(final String signupConfirmationSubject) {
    this.signupConfirmationSubject = signupConfirmationSubject;
  }

  public void setSignupConfirmationFromEmail(final String signupConfirmationFromEmail) {
    this.signupConfirmationFromEmail = signupConfirmationFromEmail;
  }

  public String holdForEmailConfirmation(final User user, final String confirmationUrl) throws RegistrationException {
    if (null == user) { throw new IllegalArgumentException("user can't be null"); }
    if (null == confirmationUrl) { throw new IllegalArgumentException("confirmationUrl can't be null"); }

    if (!userService.store(user)) { throw new RegistrationUserStorageErrorException(user, null); }

    final UUID uuid = UUID.randomUUID();
    final String uuidString = uuid.toString().replaceAll("\\W", ""); // strip away non word chars for easy copy/paste
    heldUsers.put(uuidString, user.getId());
    
    try {
      final Map model = new HashMap();
      model.put("user", user);
      model.put("url", confirmationUrl);
      model.put("code", uuidString);
      final String result = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(signupConfirmationTemplateName), model);

      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setSubject(signupConfirmationSubject);
      msg.setFrom(signupConfirmationFromEmail);
      msg.setTo(user.getEmail());
      msg.setText(result);
        mailSender.send(msg);
    } catch (Exception e) {
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