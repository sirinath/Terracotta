package org.terracotta.lassen.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.terracotta.lassen.services.UserService;

@Controller
@RequestMapping("/userslist.do")
public class ListUsersController {

  private final UserService service;

  @Autowired
  public ListUsersController(final UserService service) {
    this.service = service;
  }

  @RequestMapping(method = RequestMethod.GET)
  public ModelAndView setupForm() {
    return new ModelAndView("listusers", "users", service.getAllUsers());
  }
}