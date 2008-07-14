package org.terracotta.lassen.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.UserService;

@Controller
@RequestMapping("/editUser.do")
public class EditUserForm extends AbstractUserForm {

	@Autowired
	public EditUserForm(UserService userService) {
		super(userService);
	}

	@RequestMapping(method = RequestMethod.GET)
	public String setupForm(@RequestParam("id") int id, Model model) {
		User user = this.service.findById(id);
		model.addAttribute(user);
		return "userForm";
	}
}