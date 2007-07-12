/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.geronimo1_1.gbean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.terracotta.geronimo1_1.api.TerracottaGBean;

public final class Terracotta implements TerracottaGBean, GBeanLifecycle {

  private static final Log logger = LogFactory.getLog(Terracotta.class);

  public Terracotta() {
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.instanted());
    }
  }

  public void doStart() throws Exception {
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.starting());
    }
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.started());
    }
  }

  public void doStop() throws Exception {
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.stopping());
    }
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.stopped());
    }
  }

  public void doFail() {
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.failure());
    }
    try {
      doStop();
    } catch (Exception e) {
      logger.error(e, e);
    }
  }

  public void install() {
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.installing());
    }
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.installed());
    }
  }

  public void uninstall() {
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.uninstalling());
    }
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.uninstalled());
    }
  }

  public void enable() {
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.enabling());
    }
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.enabled());
    }
  }

  public void disable() {
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.disabling());
    }
    if (logger.isInfoEnabled()) {
      logger.info(TerracottaGBean.Messages.disabled());
    }
  }

  public static final GBeanInfo GBEAN_INFO;

  public static GBeanInfo getGBeanInfo() {
    return GBEAN_INFO;
  }

  static {
    GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic("Terracotta", Terracotta.class);
    infoFactory.addInterface(TerracottaGBean.class);
    GBEAN_INFO = infoFactory.getBeanInfo();
  }

}
