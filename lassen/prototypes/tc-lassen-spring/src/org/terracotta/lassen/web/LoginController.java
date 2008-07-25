package org.terracotta.lassen.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.security.ui.webapp.AuthenticationProcessingFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/login.do")
public class LoginController {
  @RequestMapping(method = RequestMethod.GET)
  public ModelAndView loginHandler(final HttpServletRequest request, final HttpServletResponse response, final HttpSession session) {
    final ModelAndView result = new ModelAndView("login");
    
    boolean loginError = request.getParameter("login_error") != null;
    if (loginError) {
      result.addObject("loginError", loginError);
      
      final String lastUser = (String) session.getAttribute(AuthenticationProcessingFilter.SPRING_SECURITY_LAST_USERNAME_KEY);
      final Throwable lastException = (Throwable)session.getAttribute(AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY);
      if (lastUser != null) {
        result.addObject("lastUser", lastUser);
      }
      if (lastException != null) {
        result.addObject("errorMessage", lastException.getMessage());
      }
    }
    
    return result;
  }
}