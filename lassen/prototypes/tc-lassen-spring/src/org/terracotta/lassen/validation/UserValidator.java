package org.terracotta.lassen.validation;

import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.UserService;

@Service
public class UserValidator {

  private final UserService service;

  @Autowired
  public UserValidator(final UserService userService) {
    this.service = userService;
  }

  public void validate(final User user, final Errors errors) {
    if (!StringUtils.hasLength(user.getUserName())) {
      errors.rejectValue("userName", "required", "required");
    } else if (service.findByUserName(user.getUserName()) != null) {
      errors.rejectValue("userName", "exists", "exists");
    } else if (!GenericValidator.minLength(user.getUserName(), 5)) {
      errors.rejectValue("userName", "tooshort", "tooshort");
    } else if (!GenericValidator.maxLength(user.getUserName(), 20)) {
      errors.rejectValue("userName", "toolong", "toolong");
    }

    if (!StringUtils.hasLength(user.getPassword())) {
      errors.rejectValue("password", "required", "required");
    } else if (!GenericValidator.minLength(user.getPassword(), 5)) {
      errors.rejectValue("password", "tooshort", "tooshort");
    } else if (!GenericValidator.maxLength(user.getPassword(), 20)) {
      errors.rejectValue("password", "toolong", "toolong");
    }

    if (!StringUtils.hasLength(user.getEmail())) {
      errors.rejectValue("email", "required", "required");
    } else if (!GenericValidator.isEmail(user.getEmail())) {
      errors.rejectValue("email", "invalid", "invalid");
    } else if (service.findByEmail(user.getEmail()) != null) {
      errors.rejectValue("email", "exists", "exists");
    }

    if (!StringUtils.hasLength(user.getFirstName())) {
      errors.rejectValue("firstName", "required", "required");
    }

    if (!StringUtils.hasLength(user.getLastName())) {
      errors.rejectValue("lastName", "required", "required");
    }
  }
}