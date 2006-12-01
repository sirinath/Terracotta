/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.qa.ack.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.terracotta.qa.ack.AckService;

public final class AckServlet extends HttpServlet {

  private final String ATTR_ACKSERVICE      = "ack.service";
  private final String SERVICE_NAME         = "name";
  private final String SERVICE_THREAD_COUNT = "worker-count";

  public String getServletName() {
    return "AckServlet";
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    final String pathInfo = request.getPathInfo();
    final String methodToCall = pathInfo != null ? pathInfo.replaceAll("^/*", "") : pathInfo;
    try {
      final Method servletMethod = AckServlet.class.getDeclaredMethod(methodToCall, new Class[] {
          HttpServletRequest.class, HttpServletResponse.class });
      servletMethod.invoke(this, new Object[] { request, response });
    } catch (NoSuchMethodException nsme) {
      failed(response, "Invalid PATH_INFO in URL, please use one of \"ack\" or \"init\".<p>"
                       + "When using \"init\" the query parameters \"" + SERVICE_NAME
                       + "\" (optional, default is \"DefaultService\") and \"" + SERVICE_THREAD_COUNT
                       + "\" (required) can be used to define the name and worker thread count respectivly.", nsme);
    } catch (Exception e) {
      failed(response, "Exception when trying to call method[" + methodToCall
                       + "] based on the PATH_INFO of the request", e);
    }
  }

  private AckService getOrCreateAckService() {
    final ServletContext context = getServletContext();
    AckService ackService;
    synchronized (context) {
      ackService = (AckService) context.getAttribute(ATTR_ACKSERVICE);
      if (ackService == null) {
        ackService = new AckService("DefaultService", 0);
        context.setAttribute(ATTR_ACKSERVICE, ackService);
      }
    }
    return ackService;
  }

  private void putAckService(final AckService service) {
    final ServletContext context = getServletContext();
    synchronized (context) {
      final AckService oldService = (AckService) context.getAttribute(ATTR_ACKSERVICE);
      if (oldService != null) oldService.stop();
      context.setAttribute(ATTR_ACKSERVICE, service);
    }
  }

  private void ack(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    try {
      final AckService service = getOrCreateAckService();
      final AckService.Request ackRequest = new AckService.Request();
      service.ack(ackRequest, 10 * 1000);
      if (ackRequest.acknowledger() == null) {
        success(response, "Ack request timed out after 10 seconds");
      } else {
        success(response, "Ack was received from: " + htmlTag("b", ackRequest.acknowledger()));
      }
    } catch (Throwable t) {
      failed(response, "Got an exception while trying to use the Ack service", t);
    }
  }

  private void init(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    final Integer workerThreadCount = new Integer(request.getParameter(SERVICE_THREAD_COUNT));
    String serviceName = request.getParameter(SERVICE_NAME);
    if (serviceName == null) serviceName = "DefaultService";
    putAckService(new AckService(serviceName, workerThreadCount.intValue()));
    success(response, "Created service[" + serviceName + "] with " + workerThreadCount + " threads");
  }

  private static void success(final HttpServletResponse response, final String message) throws IOException {
    final PrintWriter output = getWriter(response);
    output.print(htmlHeader("Processing complete"));
    output.print(htmlTag("body", message));
    output.print(htmlFooter());
    output.flush();
    output.close();
  }

  private static void failed(final HttpServletResponse response, final String errorMessage, final Throwable error)
      throws IOException {
    final StringWriter stackTrace = new StringWriter();
    if (error != null) error.printStackTrace(new PrintWriter(stackTrace));
    stackTrace.flush();

    final PrintWriter output = getWriter(response);
    output.print(htmlHeader("Application and/or user error"));
    output.print(htmlTag("body", htmlTag("h1", errorMessage) + "<br>" + htmlTag("verbatim", stackTrace.toString())));
    output.print(htmlFooter());
    output.flush();
    output.close();
  }

  private static String htmlHeader(final String title) {
    return new StringBuffer("<html>").append(htmlTag("head", htmlTag("title", title))).toString();
  }

  private static String htmlFooter() {
    return "</html>";
  }

  private static String htmlTag(final String tag, final String contents) {
    if (contents == null || "".equals(contents.trim())) return "<" + tag + " />";
    else return new StringBuffer("<").append(tag).append(">").append(contents).append("</" + tag + ">").toString();
  }

  private static PrintWriter getWriter(final HttpServletResponse response) throws IOException {
    final Writer rawWriter = response.getWriter();
    return rawWriter instanceof PrintWriter ? (PrintWriter) rawWriter : new PrintWriter(rawWriter);
  }

}
