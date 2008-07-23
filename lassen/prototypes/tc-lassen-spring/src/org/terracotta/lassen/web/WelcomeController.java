package org.terracotta.lassen.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/welcome.do")
public class WelcomeController {
  @RequestMapping(method = RequestMethod.GET)
  public void welcomeHandler() {
    // no controller logic, just display the view
  }
}