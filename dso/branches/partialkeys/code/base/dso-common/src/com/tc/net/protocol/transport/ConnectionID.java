/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.tcm.ChannelID;

public class ConnectionID {

  private final long               channelID;
  private final String             serverID;

  private static final String      NULL_SERVER_ID = "ffffffffffffffffffffffffffffffff";
  public static final ConnectionID NULL_ID        = new ConnectionID(ChannelID.NULL_ID.toLong(), NULL_SERVER_ID);

  private static final String      SEP            = ".";

  public static ConnectionID parse(String compositeID) throws InvalidConnectionIDException {
    if (compositeID == null) { throw new InvalidConnectionIDException(compositeID, "null connectionID"); }

    String[] parts = compositeID.split("\\" + SEP);
    if (parts.length != 2) {
      // make formatter sane
      throw new InvalidConnectionIDException(compositeID, "wrong number of components: " + parts.length);
    }

    String channelID = parts[0];
    final long channel;
    try {
      channel = Long.parseLong(channelID);
    } catch (Exception e) {
      throw new InvalidConnectionIDException(compositeID, "parse exception for channelID " + channelID, e);
    }

    String server = parts[1];
    if (server.length() != 32) { throw new InvalidConnectionIDException(compositeID, "invalid serverID length: "
                                                                                     + server.length()); }

    if (!server.matches("[A-Fa-f0-9]+")) { throw new InvalidConnectionIDException(compositeID,
                                                                                  "invalid chars in serverID: "
                                                                                      + server); }

    return new ConnectionID(channel, server);
  }

  public ConnectionID(long channelID, String serverID) {
    this.channelID = channelID;
    this.serverID = serverID;
  }

  public ConnectionID(long channelID) {
    this(channelID, NULL_SERVER_ID);
  }

  public String toString() {
    return "ConnectionID(" + getID() + ")";
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
  }

  public boolean isNewConnection() {
    return (serverID.equals(NULL_SERVER_ID));
  }

  public String getServerID() {
    return this.serverID;
  }

  public int hashCode() {
    int hc = 17;
    hc = 37 * hc + (int) (this.channelID ^ (this.channelID >>> 32));
    if (this.serverID != null) {
      hc = 37 * hc + serverID.hashCode();
    }

    return hc;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ConnectionID) {
      ConnectionID other = (ConnectionID) obj;
      return (this.channelID == other.channelID) && (this.serverID.equals(other.serverID));
    }
    return false;
  }

  public long getChannelID() {
    return channelID;
  }

  public String getID() {
    return channelID + SEP + serverID;
  }

}
