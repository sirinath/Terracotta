package org.terracotta.lassen.validation;

import org.apache.commons.validator.EmailValidator;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.terracotta.lassen.models.User;

public class UserValidator {
	public void validate(User user, Errors errors) {
		if (!StringUtils.hasLength(user.getUsername())) {
			errors.rejectValue("username", "required", "required");
		}
		if (!StringUtils.hasLength(user.getPassword())) {
			errors.rejectValue("password", "required", "required");
		}
		if (!StringUtils.hasLength(user.getEmail())) {
			errors.rejectValue("email", "required", "required");
		}
		if (!EmailValidator.getInstance().isValid(user.getEmail())) {
			errors.rejectValue("email", "invalid", "invalid");
		}
	}

}
