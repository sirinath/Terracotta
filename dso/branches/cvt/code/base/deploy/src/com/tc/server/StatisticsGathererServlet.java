/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.impl.StatisticsGathererImpl;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.StatisticData;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that provides a RESTful interface towards an embedded statistics gatherer
 */
public class StatisticsGathererServlet extends RestfulServlet {
  private static final TCLogger logger        = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private volatile L2TVSConfigurationSetupManager configSetupManager;
  private volatile StatisticsStore store;
  private volatile StatisticsGatherer gatherer;

  public void init() {
    configSetupManager = (L2TVSConfigurationSetupManager)getServletContext().getAttribute(ConfigServlet.CONFIG_ATTRIBUTE);

    File stat_path = configSetupManager.commonl2Config().statisticsPath().getFile();
    store = new H2StatisticsStoreImpl(stat_path);
    gatherer = new StatisticsGathererImpl(store);
    String infoMsg = "Statistics store: '" + stat_path.getAbsolutePath() + "'.";
    consoleLogger.info(infoMsg);
    logger.info(infoMsg);
  }

  public void methodConnect(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    gatherer.connect(TCSocketAddress.LOOPBACK_IP, configSetupManager.commonl2Config().jmxPort().getInt());
    printOk(response);
  }

  public void methodDisconnect(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    gatherer.disconnect();
    printOk(response);
  }

  public void methodCreateSession(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String sessionid = request.getParameter("sessionId");
    if (null == sessionid) throw new IllegalArgumentException("sessionId");
    gatherer.createSession(sessionid);
    printOk(response);
  }

  public void methodCloseSession(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    gatherer.closeSession();
    printOk(response);
  }

  public void methodGetSupportedStatistics(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String[] statistics = gatherer.getSupportedStatistics();
    print(response, statistics);
  }

  public void methodEnableStatistics(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String[] names = request.getParameterValues("names");
    if (null == names) throw new IllegalArgumentException("names");
    gatherer.enableStatistics(names);
    printOk(response);
  }

  public void methodStartCapturing(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    gatherer.startCapturing();
    printOk(response);
  }

  public void methodStopCapturing(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    gatherer.stopCapturing();
    printOk(response);
  }

  public void methodSetGlobalParam(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String key = request.getParameter("key");
    String value = request.getParameter("value");
    if (null == key) throw new IllegalArgumentException("key");
    if (null == value) throw new IllegalArgumentException("value");
    gatherer.setGlobalParam(key, value);
    printOk(response);
  }

  public void methodGetGlobalParam(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String key = request.getParameter("key");
    if (null == key) throw new IllegalArgumentException("key");
    Object value = gatherer.getGlobalParam(key);
    print(response, value);
  }

  public void methodSetSessionParam(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String key = request.getParameter("key");
    String value = request.getParameter("value");
    if (null == key) throw new IllegalArgumentException("key");
    if (null == value) throw new IllegalArgumentException("value");
    gatherer.setSessionParam(key, value);
    printOk(response);
  }

  public void methodGetSessionParam(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String key = request.getParameter("key");
    if (null == key) throw new IllegalArgumentException("key");
    Object value = gatherer.getSessionParam(key);
    print(response, value);
  }

  public void methodGetAvailableSessionIds(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String[] sessionids = store.getAvailableSessionIds();
    print(response, sessionids);
  }

  public void methodClearStatistics(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String sessionid = request.getParameter("sessionId");
    if (null == sessionid) throw new IllegalArgumentException("sessionId");
    store.clearStatistics(sessionid);
    printOk(response);
  }

  public void methodRetrieveStatistics(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    final StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria()
      .sessionId(request.getParameter("sessionId"))
      .agentIp(request.getParameter("agentIp"))
      .agentDifferentiator(request.getParameter("agentDifferentiator"))
      .setNames(request.getParameterValues("names"))
      .setElements(request.getParameterValues("elements"));

    final boolean textformat = "txt".equals(request.getParameter("format"));

    DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    String filename_base = "statistics-" + format.format(new Date());

    if (textformat) {
      response.setContentType("text/plain");
    } else {
      response.setHeader("Content-Disposition", "attachment; filename=\"" + filename_base + ".zip\"");
      response.setContentType("application/zip");
    }
    response.setStatus(HttpServletResponse.SC_OK);

    OutputStream os = response.getOutputStream();
    final OutputStream out;

    try {
      final ZipOutputStream zipstream;
      if (textformat) {
        zipstream = null;
        out = os;
      } else {
        zipstream = new ZipOutputStream(os);
        zipstream.setLevel(9);
        zipstream.setMethod(ZipOutputStream.DEFLATED);
        out = zipstream;
      }

      try {
        if (zipstream != null) {
          final ZipEntry zipentry = new ZipEntry(filename_base + ".csv");
          zipstream.putNextEntry(zipentry);
        }

        try {
          store.retrieveStatistics(criteria, new StatisticsConsumer() {
            public boolean consumeStatisticData(final StatisticData data) {
              try {
                out.write(data.toCsv().getBytes("UTF-8"));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            }
          });
        } finally {
          if (zipstream != null) {
            zipstream.closeEntry();
          }
        }
      } finally {
        if (zipstream != null) {
          zipstream.close();
        }
      }
    } finally {
      os.close();
    }
  }
}