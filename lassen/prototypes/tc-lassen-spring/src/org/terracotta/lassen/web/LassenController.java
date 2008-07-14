package org.terracotta.lassen.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LassenController {
	@RequestMapping("/welcome.do")
	public void welcomeHandler() {
	}
}