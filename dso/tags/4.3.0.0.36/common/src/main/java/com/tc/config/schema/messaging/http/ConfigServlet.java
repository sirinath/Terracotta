/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConfigServlet extends HttpServlet {

  public static final String                   CONFIG_ATTRIBUTE = ConfigServlet.class.getName() + ".config";

  private volatile L2ConfigurationSetupManager configSetupManager;

  @Override
  public void init() {
    configSetupManager = (L2ConfigurationSetupManager) getServletContext().getAttribute(CONFIG_ATTRIBUTE);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Map params = request.getParameterMap();

    if (params.size() == 0) {
      OutputStream out = response.getOutputStream();
      IOUtils.copy(this.configSetupManager.effectiveConfigFile(), out);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      PrintWriter writer = response.getWriter();
      writer.println("request not understood");
    }

    response.flushBuffer();
  }
}
