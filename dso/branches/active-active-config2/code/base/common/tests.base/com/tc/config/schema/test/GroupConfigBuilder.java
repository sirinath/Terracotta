/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

public class GroupConfigBuilder extends BaseConfigBuilder {

  private int                  id = -1;
  private MembersConfigBuilder members;
  private HaConfigBuilder      ha;

  public GroupConfigBuilder() {
    super(5, new String[0]);
  }

  public void setId(int data) {
    this.id = data;
  }

  String getId() {
    return this.id + "";
  }

  public void setMembers(MembersConfigBuilder members) {
    this.members = members;
  }

  public void setHa(HaConfigBuilder ha) {
    this.ha = ha;
  }

  public String toString() {
    String out = "";

    out += indent() + "<active-server-group" + (this.id >= 0 ? " id=\"" + this.id + "\"" : "") + ">\n";

    out += this.members.toString();

    out += this.ha.toString();

    out += closeElement("active-server-group");

    return out;
  }
}
