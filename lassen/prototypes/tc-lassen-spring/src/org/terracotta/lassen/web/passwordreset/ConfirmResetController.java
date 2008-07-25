package org.terracotta.lassen.web.passwordreset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.terracotta.lassen.models.UserCodeConfirmation;
import org.terracotta.lassen.services.PasswordResetService;
import org.terracotta.lassen.services.exceptions.PasswordResetException;

@Controller
@RequestMapping("/passwordreset/confirm.do")
public class ConfirmResetController {

  private final PasswordResetService service;

  @Autowired
  public ConfirmResetController(final PasswordResetService service) {
    this.service = service;
  }

  @RequestMapping(method = RequestMethod.GET)
  public String setupForm() {
    return "passwordreset/confirm";
  }

  @RequestMapping(method = RequestMethod.POST)
  public ModelAndView processSubmit(@ModelAttribute final UserCodeConfirmation confirmation) throws PasswordResetException {
    if (service.generateNewPassword(confirmation.getCode())) {
      return new ModelAndView("passwordreset/success");
    } else {
      return new ModelAndView("passwordreset/confirm", "invalid", true);
    }
  }
}