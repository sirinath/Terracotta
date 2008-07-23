package org.terracotta.lassen.web.registration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.terracotta.lassen.models.SignupConfirmation;
import org.terracotta.lassen.services.RegistrationService;
import org.terracotta.lassen.services.exceptions.RegistrationException;

@Controller
@RequestMapping("/registration/confirm.do")
public class ConfirmController {

  private final RegistrationService service;

  @Autowired
  public ConfirmController(final RegistrationService userService) {
    this.service = userService;
  }

  @RequestMapping(method = RequestMethod.GET)
  public String setupForm() {
    return "registration/confirm";
  }

  @RequestMapping(method = RequestMethod.POST)
  public ModelAndView processSubmit(@ModelAttribute final SignupConfirmation confirmation) throws RegistrationException {
    if (service.confirmRegistration(confirmation)) {
      return new ModelAndView("registration/finalized");
    } else {
      return new ModelAndView("registration/confirm", "invalid", true);
    }
  }
}