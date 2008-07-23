package org.terracotta.lassen.web.registration;

import java.net.ProtocolException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.RegistrationService;
import org.terracotta.lassen.services.exceptions.RegistrationException;
import org.terracotta.lassen.util.UrlHelper;
import org.terracotta.lassen.validation.UserValidator;

@Controller
@RequestMapping("/registration/signup.do")
@SessionAttributes(types = User.class)
public class SignupController {

  private final RegistrationService service;
  private final UserValidator       validator;

  @Autowired
  public SignupController(final RegistrationService userService, final UserValidator userValidator) {
    this.service = userService;
    this.validator = userValidator;
  }

  @RequestMapping(method = RequestMethod.GET)
  public String setupForm(final Model model) {
    User user = new User();
    model.addAttribute(user);
    return "registration/signup";
  }

  @RequestMapping(method = RequestMethod.POST)
  public ModelAndView processSubmit(final HttpServletRequest request, @ModelAttribute final User user,
                                    final BindingResult result, final SessionStatus status) throws RegistrationException, ProtocolException {
    validator.validate(user, result);
    if (result.hasErrors()) {
      return new ModelAndView("registration/signup");
    } else {
      this.service.holdForEmailConfirmation(user, UrlHelper.createAbsolutePrettyUrl(request, ConfirmController.class));
      status.setComplete();
      return new ModelAndView("registration/success", "user", user);
    }
  }
}