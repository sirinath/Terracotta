/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.tcm.ChannelID;

public class ConnectionID {

  private static final char        DOT_PLACEHOLDER = '+';
  private static final char        DOT             = '.';

  private final long               channelID;
  private final String             serverID;
  private final String             jvmID;
  private final Exception          initEx;
  private final String             username;

  private volatile char[]          password;

  private static final String      NULL_SERVER_ID = "ffffffffffffffffffffffffffffffff";
  public static final String       NULL_JVM_ID    = "ffffffffffffffffffffffffffffffffffffffffffffffff";
  public static final ConnectionID NULL_ID        = new ConnectionID(NULL_JVM_ID, ChannelID.NULL_ID.toLong(),
                                                                     NULL_SERVER_ID);

  private static final char        SEP            = '.';

  public static ConnectionID parse(String compositeID) throws InvalidConnectionIDException {

    if (compositeID == null) { throw new InvalidConnectionIDException("NULL ConnectionID"); }

    int idx = compositeID.indexOf(SEP);
    if (idx <= 0 || idx >= compositeID.length() - 1) {
      // make formatter sane
      throw new InvalidConnectionIDException(compositeID, "Invalid format. Separator (.) found at : " + idx);
    }
    int idx2 = compositeID.indexOf(SEP, idx + 1);
    if (idx2 <= idx + 1 || idx2 >= compositeID.length() - 1) {
      // make formatter sane
      throw new InvalidConnectionIDException(compositeID, "Invalid format. Separator (.) found at : " + idx2);
    }

    String channelID = compositeID.substring(0, idx);
    final long channel;
    try {
      channel = Long.parseLong(channelID);
    } catch (Exception e) {
      throw new InvalidConnectionIDException(compositeID, "parse exception for channelID " + channelID, e);
    }

    String server = compositeID.substring(idx + 1, idx2);
    if (server.length() != 32) { throw new InvalidConnectionIDException(compositeID, "invalid serverID length: "
                                                                                     + server.length()); }

    int idx3 = compositeID.indexOf(SEP, idx2 + 1);
    int idx4 = compositeID.indexOf(SEP, idx3 + 1);
    String jvmID = compositeID.substring(idx2 + 1, idx3);
    if (jvmID.length() < 1) { throw new InvalidConnectionIDException(compositeID, "invalid jvmId length: "
                                                                                  + jvmID.length()); }

    if (!validateCharsInServerID(server)) { throw new InvalidConnectionIDException(compositeID,
                                                                                   "invalid chars in serverID: "
                                                                                       + server); }
    String username = null;
    char[] password = null;
    if (idx3 != idx4 - 1) {
      username = compositeID.substring(idx3 + 1, idx4).replace(DOT_PLACEHOLDER, DOT);
      password = compositeID.substring(idx4 + 1).toCharArray();
    }

    return new ConnectionID(jvmID, channel, server, username, password);
  }

  /**
   * This method does not use String.matches() for performance reason.
   */
  private static boolean validateCharsInServerID(String server) {
    for (int i = 0; i < server.length(); i++) {
      char c = server.charAt(i);
      if (!(((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'f')) || ((c >= 'A') && (c <= 'F')))) { return false; }
    }
    return true;
  }

  public ConnectionID(String jvmID, long channelID, String serverID) {
    this(jvmID, channelID, serverID, null, (char[]) null);
  }

  public ConnectionID(String jvmID, long channelID, String serverID, String username, String password) {
    this(jvmID, channelID, serverID, username, password == null ? null : password.toCharArray());
  }

  public ConnectionID(String jvmID, long channelID, String serverID, String username, char[] password) {
    this.jvmID = jvmID;
    this.channelID = channelID;
    this.serverID = serverID;

    if (jvmID.equals(NULL_JVM_ID)) {
      initEx = new Exception("Created (" + getID() + ") by:-----------------------------------------------------------");
    } else {
      initEx = null;
    }
    this.username = username;
    this.password = password;
  }

  public void authenticated() {
    this.password = null;
  }

  public void setPassword(final char[] password) {
    this.password = password;
  }

  public ConnectionID(String jvmID, long channelID, String username, char[] password) {
    this(jvmID, channelID, NULL_SERVER_ID, username, password);
  }

  public ConnectionID(String jvmID, long channelID) {
    this(jvmID, channelID, NULL_SERVER_ID);
  }

  @Override
  public String toString() {
    return "ConnectionID" + (isSecured() ? ".secured(" : "(") + getID() + ")[" + "]";
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
  }

  public boolean isNewConnection() {
    return (this.serverID.equals(NULL_SERVER_ID));
  }

  public String getServerID() {
    return this.serverID;
  }

  @Override
  public int hashCode() {
    int hc = 17;
    hc = 37 * hc + (int) (this.channelID ^ (this.channelID >>> 32));
    if (this.serverID != null) {
      hc = 37 * hc + this.serverID.hashCode();
    }

    return hc;
  }

  @Override
  public boolean equals(Object obj) {
    // equals does NOT take into account jvmID on purpose.
    // there are cases where we do not have a value (cases where a ConnectionID
    // can only be built with a channelID ... true identification is based
    // on channelId
    if (obj instanceof ConnectionID) {
      ConnectionID other = (ConnectionID) obj;
      return (this.channelID == other.channelID) && (this.serverID.equals(other.serverID));
    }
    return false;
  }

  public long getChannelID() {
    return this.channelID;
  }

  public String getJvmID() {
    if (this.jvmID.equals(NULL_JVM_ID)) { throw new IllegalStateException(
                                                                          "Attempt to get jvmID from pseudo-ConnectionID that was not initialized with one.",
                                                                          initEx); }
    return this.jvmID;
  }

  public boolean isJvmIDNull() {
    return this.jvmID.equals(NULL_JVM_ID);
  }

  public String getID() {
    return getID(false);
  }

  public String getID(final boolean withCredentials) {
    StringBuilder sb = new StringBuilder(withCredentials ? 128 : 64);
    sb.append(this.channelID).append(SEP).append(this.serverID).append(SEP).append(this.jvmID);
    if (withCredentials) {
      sb.append(SEP);
      if(username != null) {
        sb.append(username.replace(DOT, DOT_PLACEHOLDER));
      }
      sb.append(SEP);
      if(password != null) {
        sb.append(password);
      }
    }
    return sb.toString();
  }

  public String getUsername() {
    return username;
  }

  public char[] getPassword() {
    return password;
  }

  public boolean isSecured() {
    return username != null;
  }
}
