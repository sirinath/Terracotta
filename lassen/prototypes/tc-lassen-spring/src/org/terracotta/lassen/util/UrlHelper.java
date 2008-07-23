package org.terracotta.lassen.util;

import java.net.ProtocolException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;

public abstract class UrlHelper {
  public static String createAbsolutePrettyUrl(final HttpServletRequest request, final String location) throws ProtocolException {
    final StringBuilder result = new StringBuilder();
    final String protocol = request.getProtocol().toUpperCase();
    if (protocol.startsWith("HTTPS")) {
      result.append("https");
    } else if (protocol.startsWith("HTTP")) {
      result.append("http");
    } else {
      throw new ProtocolException("Protocol " + protocol + " isn't supported");
    }
    result.append("://");
    result.append(request.getServerName());
    final int port = request.getServerPort();
    if (port != 80) {
      result.append(":");
      result.append(port);
    }
    result.append(request.getContextPath());
    if (!location.startsWith("/")) {
      result.append("/");
    }
    result.append(location);
    return result.toString();
  }
  
  public static String createAbsolutePrettyUrl(final HttpServletRequest request, final Class controllerClass) throws ProtocolException {
    return createAbsolutePrettyUrl(request, getControllerRequestMapping(controllerClass));
  }
  
  public static String getControllerRequestMapping(final Class controllerClass) {
    if (null == controllerClass) {
      return null;
    }
    RequestMapping mapping = (RequestMapping)controllerClass.getAnnotation(RequestMapping.class);
    if (null == mapping) {
      return null;
    }
    String[] value = mapping.value();
    if (null == value ||
        0 == value.length) {
      return null;
    }
    return value[0];
  }
}