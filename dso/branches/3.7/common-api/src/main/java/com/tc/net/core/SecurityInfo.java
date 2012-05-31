package com.tc.net.core;

/**
 * @author Alex Snaps
 */
public class SecurityInfo {

  private final boolean secure;
  private final String username;

  public SecurityInfo() {
    this(false, null);
  }

  public SecurityInfo(final boolean secure, final String username) {
    this.secure = secure;
    this.username = username;
  }

  public boolean isSecure() {
    return secure;
  }

  public String getUsername() {
    return username;
  }
}
