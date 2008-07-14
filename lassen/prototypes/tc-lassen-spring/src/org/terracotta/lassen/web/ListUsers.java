package org.terracotta.lassen.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.terracotta.lassen.services.UserService;

@Controller
@RequestMapping("/listUsers.do")
public class ListUsers {

	private final UserService service;

	@Autowired
	public ListUsers(UserService userService) {
		this.service = userService;
	}

	@RequestMapping(method = RequestMethod.GET)
	public ModelMap setupForm(Model model) {
		return new ModelMap("users", service.getAllUsers());
	}
}