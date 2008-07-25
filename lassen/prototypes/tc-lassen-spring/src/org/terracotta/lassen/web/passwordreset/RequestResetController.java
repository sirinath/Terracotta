package org.terracotta.lassen.web.passwordreset;

import java.net.ProtocolException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.terracotta.lassen.services.PasswordResetService;
import org.terracotta.lassen.services.exceptions.PasswordResetException;
import org.terracotta.lassen.util.UrlHelper;

@Controller
@RequestMapping("/passwordreset/request.do")
public class RequestResetController {

  private final PasswordResetService service;

  @Autowired
  public RequestResetController(final PasswordResetService service) {
    this.service = service;
  }

  @RequestMapping(method = RequestMethod.GET)
  public ModelAndView setupForm() {
    return new ModelAndView("passwordreset/request");
  }

  @RequestMapping(method = RequestMethod.POST)
  public ModelAndView processSubmit(final HttpServletRequest request, @RequestParam String email) throws ProtocolException, PasswordResetException {
    final boolean found = service.requestConfirmation(email, UrlHelper.createAbsolutePrettyUrl(request, ConfirmResetController.class));
    if (found) {
      return new ModelAndView("passwordreset/sent");
    } else {
      return new ModelAndView("passwordreset/request", "userNotFound", true);
    }
  }
}