/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

/**
 * Allows you to build valid config for the L2s. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class L2SConfigBuilder extends BaseConfigBuilder {

  private L2ConfigBuilder[] l2ConfigBuilders;
  private HaConfigBuilder haConfigBuilder;

  public L2SConfigBuilder() {
    super(1, new String[] { "servers" });
  }

  public void setL2s(L2ConfigBuilder[] l2s) {
    l2ConfigBuilders = l2s;
    setServersProperty();
  }
  
  public void setHa(HaConfigBuilder ha) {
    haConfigBuilder = ha;
   setServersProperty();
  }

  private void setServersProperty() {
    String val = "";
    if (l2ConfigBuilders != null) {
      val += selfTaggingArray(l2ConfigBuilders).toString();
    }
    if (haConfigBuilder != null) {
      val += haConfigBuilder.toString();
    }
    setProperty("servers", val);
  }

  public L2ConfigBuilder[] getL2s() {
    return l2ConfigBuilders;
  }
  
  public HaConfigBuilder getHa() {
    return haConfigBuilder;
  }

  public String toString() {
    if (!isSet("servers")) return "";
    else return getProperty("servers").toString();
  }

  public static L2SConfigBuilder newMinimalInstance() {
    L2ConfigBuilder l2 = new L2ConfigBuilder();
    l2.setName("localhost");
    l2.setDSOPort(9510);

    HaConfigBuilder ha = new HaConfigBuilder();

    L2SConfigBuilder out = new L2SConfigBuilder();
    out.setL2s(new L2ConfigBuilder[] { l2 });
    out.setHa(ha);

    return out;
  }

}
