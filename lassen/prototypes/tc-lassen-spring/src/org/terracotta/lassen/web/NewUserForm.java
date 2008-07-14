package org.terracotta.lassen.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.UserService;

@Controller
@RequestMapping("/register.do")
public class NewUserForm extends AbstractUserForm {

	@Autowired
	public NewUserForm(UserService userService) {
		super(userService);
	}

	@RequestMapping(method = RequestMethod.GET)
	public String setupForm(Model model) {
		User user = new User();
		model.addAttribute(user);
		return "userForm";
	}
}