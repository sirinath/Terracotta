/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.NewStatisticsConfig;
import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.util.ZipBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that exports an archive of the stats db directory for the requested capture session.
 */

public class StatsExportServlet extends HttpServlet {
  private volatile L2TVSConfigurationSetupManager configSetupManager;

  public void init() {
    configSetupManager = (L2TVSConfigurationSetupManager) getServletContext()
        .getAttribute(ConfigServlet.CONFIG_ATTRIBUTE);
  }

  protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Should return archive of database for sessionId
    long sessionId = Long.parseLong(request.getParameter("session"));
    NewStatisticsConfig config = configSetupManager.commonl2Config();
    File statsDir = config.statisticsPath().getFile();
    File outFile = File.createTempFile("tc-stats", ".zip");
    outFile.deleteOnExit();
    ZipBuilder builder = new ZipBuilder(outFile, true);
    builder.putTraverseDirectory(statsDir, statsDir.getName());
    builder.finish();
    response.setContentLength((int) outFile.length());
    response.setContentType("application/zip");
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(outFile);
      IOUtils.copy(fis, response.getOutputStream());
    } finally {
      IOUtils.closeQuietly(fis);
      if (!outFile.delete()) { throw new IOException("Can't delete '" + outFile.getAbsolutePath() + "'"); }
    }
  }
}
