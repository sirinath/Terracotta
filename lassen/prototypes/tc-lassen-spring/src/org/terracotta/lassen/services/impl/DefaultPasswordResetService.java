package org.terracotta.lassen.services.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.PasswordResetService;
import org.terracotta.lassen.services.UserService;
import org.terracotta.lassen.services.exceptions.PasswordResetConfirmationEmailException;
import org.terracotta.lassen.services.exceptions.PasswordResetDoneEmailException;
import org.terracotta.lassen.services.exceptions.PasswordResetException;
import org.terracotta.lassen.util.SecurityHelper;

import freemarker.template.Configuration;

//todo: implement purging of password reset requests that are expired
@Service
public class DefaultPasswordResetService implements PasswordResetService {

  private final Configuration     freemarkerConfiguration;
  private final UserService       userService;
  private final MailSender        mailSender;
  private final Map<String, Long> resetCodes = new ConcurrentHashMap<String, Long>(); // todo: this should be a weak hashmap at least, have to check how this fares with DSO

  private String  requestConfirmationTemplateName;
  private String  requestConfirmationSubject;
  private String  requestConfirmationFromEmail;
  private String  resetDoneTemplateName;
  private String  resetDoneSubject;
  private String  resetDoneFromEmail;

  public boolean requestConfirmation(final String email, final String confirmationUrl) throws PasswordResetException {
    final User user = userService.findByEmail(email);
    if (null == user) {
      return false;
    }

    final String uuidString = SecurityHelper.generateUniqueCode();
    resetCodes.put(uuidString, user.getId());
    
    try {
      final Map model = new HashMap();
      model.put("user", user);
      model.put("url", confirmationUrl);
      model.put("code", uuidString);
      final String result = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(requestConfirmationTemplateName), model);

      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setSubject(requestConfirmationSubject);
      msg.setFrom(requestConfirmationFromEmail);
      msg.setTo(user.getEmail());
      msg.setText(result);
      mailSender.send(msg);
    } catch (Exception e) {
      throw new PasswordResetConfirmationEmailException(email, uuidString, e);
    }

    Logger.getLogger(DefaultPasswordResetService.class).debug("Password reset code: " + uuidString);

    return true;
  }

  public boolean generateNewPassword(final String code) throws PasswordResetException {
    final Long userId = resetCodes.remove(code);
    if (null == userId) {
      return false;
    }
    
    final User user = userService.findById(userId);
    if (null == user) {
      return false;
    }
    
    final String uuidString = SecurityHelper.generateUniqueCode();
    
    try {
      final Map model = new HashMap();
      model.put("user", user);
      model.put("newPassword", uuidString);
      final String result = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(resetDoneTemplateName), model);

      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setSubject(resetDoneSubject);
      msg.setFrom(resetDoneFromEmail);
      msg.setTo(user.getEmail());
      msg.setText(result);
      mailSender.send(msg);
    } catch (Exception e) {
      throw new PasswordResetDoneEmailException(user.getEmail(), e);
    }

    user.setAndEncodePassword(uuidString);
    userService.store(user);

    Logger.getLogger(DefaultPasswordResetService.class).debug("New password: " + uuidString);
    
    return true;
  }

  @Autowired
  public DefaultPasswordResetService(final Configuration freemarkerConfiguration, final UserService userService, final MailSender mailSender) {
    this.freemarkerConfiguration = freemarkerConfiguration;
    this.userService = userService;
    this.mailSender = mailSender;
  }

  public void setRequestConfirmationTemplateName(final String requestConfirmationTemplateName) {
    this.requestConfirmationTemplateName = requestConfirmationTemplateName;
  }

  public void setRequestConfirmationSubject(final String requestConfirmationSubject) {
    this.requestConfirmationSubject = requestConfirmationSubject;
  }

  public void setRequestConfirmationFromEmail(final String requestConfirmationFromEmail) {
    this.requestConfirmationFromEmail = requestConfirmationFromEmail;
  }

  public void setResetDoneTemplateName(final String resetDoneTemplateName) {
    this.resetDoneTemplateName = resetDoneTemplateName;
  }

  public void setResetDoneSubject(final String resetDoneSubject) {
    this.resetDoneSubject = resetDoneSubject;
  }

  public void setResetDoneFromEmail(final String resetDoneFromEmail) {
    this.resetDoneFromEmail = resetDoneFromEmail;
  }
}