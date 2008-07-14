package org.terracotta.lassen.web;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.UserService;
import org.terracotta.lassen.validation.UserValidator;

@SessionAttributes(types = User.class)
public abstract class AbstractUserForm {

	protected final UserService service;

	public AbstractUserForm(UserService userService) {
		this.service = userService;
	}

	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView processSubmit(@ModelAttribute User user, BindingResult result, SessionStatus status) {
		new UserValidator().validate(user, result);
		if (result.hasErrors()) {
			return new ModelAndView("userForm");
		} else {
			this.service.store(user);
			status.setComplete();
			return new ModelAndView("success", "user", user);
		}
	}
}