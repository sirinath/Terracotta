/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.admin.model.IClusterModel.PollScope;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.statistics.StatisticData;
import com.tc.stats.DSOClientMBean;
import com.tc.util.ProductInfo;

import java.beans.PropertyChangeEvent;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class DSOClient extends BaseClusterNode implements IClient, NotificationListener {
  private final ConnectionContext     cc;
  private final ObjectName            beanName;
  private final ClientID              clientId;
  private final IClusterModel         clusterModel;
  private final DSOClientMBean        delegate;
  private final long                  channelId;
  private final String                remoteAddress;
  private String                      host;
  private Integer                     port;
  protected ProductVersion            productInfo;

  private boolean                     ready;
  private boolean                     isListeningForTunneledBeans;
  private L1InfoMBean                 l1InfoBean;
  private InstrumentationLoggingMBean instrumentationLoggingBean;
  private RuntimeLoggingMBean         runtimeLoggingBean;
  private RuntimeOutputOptionsMBean   runtimeOutputOptionsBean;

  public DSOClient(ConnectionContext cc, ObjectName beanName, IClusterModel clusterModel) {
    this.cc = cc;
    this.beanName = beanName;
    this.clusterModel = clusterModel;
    this.delegate = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, beanName, DSOClientMBean.class, true);
    channelId = delegate.getChannelID().toLong();
    clientId = delegate.getClientID();
    remoteAddress = delegate.getRemoteAddress();

    initPolledAttributes();
    testSetupTunneledBeans();
  }

  private void testSetupTunneledBeans() {
    if (delegate.isTunneledBeansRegistered()) {
      setupTunneledBeans();
    } else {
      startListeningForTunneledBeans();
    }
  }

  public ObjectName getTunneledBeanName(ObjectName on) {
    try {
      String name = on.getCanonicalName() + ",clients=Clients,node=" + getRemoteAddress().replace(':', '/');
      return new ObjectName(name);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException("Creating ObjectName", mone);
    }
  }

  private void setupTunneledBeans() {
    l1InfoBean = (L1InfoMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, delegate.getL1InfoBeanName(),
                                                                             L1InfoMBean.class, true);
    addMBeanNotificationListener(delegate.getL1InfoBeanName(), this, "L1InfoMBean");

    instrumentationLoggingBean = (InstrumentationLoggingMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, delegate.getInstrumentationLoggingBeanName(), InstrumentationLoggingMBean.class,
                          true);
    addMBeanNotificationListener(delegate.getInstrumentationLoggingBeanName(), this, "InstrumentationLoggingMBean");

    runtimeLoggingBean = (RuntimeLoggingMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, delegate
        .getRuntimeLoggingBeanName(), RuntimeLoggingMBean.class, true);
    addMBeanNotificationListener(delegate.getRuntimeLoggingBeanName(), this, "RuntimeLoggingMBean");

    runtimeOutputOptionsBean = (RuntimeOutputOptionsMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, delegate.getRuntimeOutputOptionsBeanName(), RuntimeOutputOptionsMBean.class, true);
    addMBeanNotificationListener(delegate.getRuntimeOutputOptionsBeanName(), this, "RuntimeOutputOptionsMBean");

    fireTunneledBeansRegistered();
  }

  private synchronized boolean isListeningForTunneledBeans() {
    return isListeningForTunneledBeans;
  }

  private synchronized void setListeningForTunneledBeans(boolean listening) {
    isListeningForTunneledBeans = listening;
  }

  private void startListeningForTunneledBeans() {
    if (isListeningForTunneledBeans()) return;
    addMBeanNotificationListener(beanName, this, "DSOClientMBean");
    setListeningForTunneledBeans(true);
  }

  private void safeRemoveNotificationListener(ObjectName objectName, NotificationListener listener) {
    try {
      cc.removeNotificationListener(objectName, listener);
    } catch (Exception e) {
      /**/
    }
  }

  private void addMBeanNotificationListener(ObjectName objectName, NotificationListener listener, String beanType) {
    safeRemoveNotificationListener(objectName, listener);
    try {
      cc.addNotificationListener(objectName, listener);
    } catch (Exception e) {
      throw new RuntimeException("Adding listener to " + beanType, e);
    }
  }

  private void stopListeningForTunneledBeans() {
    if (!isListeningForTunneledBeans()) return;
    setListeningForTunneledBeans(false);
    try {
      cc.removeNotificationListener(beanName, this);
    } catch (Exception e) {
      throw new RuntimeException("Removing listener from DSOClientMBean", e);
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (DSOClientMBean.TUNNELED_BEANS_REGISTERED.equals(type) && isListeningForTunneledBeans()) {
      stopListeningForTunneledBeans();
      setupTunneledBeans();
    } else if (type.startsWith("tc.logging.")) {
      Boolean newValue = Boolean.valueOf(notification.getMessage());
      Boolean oldValue = Boolean.valueOf(!newValue.booleanValue());
      PropertyChangeEvent pce = new PropertyChangeEvent(this, type, oldValue, newValue);
      propertyChangeSupport.firePropertyChange(pce);
    } else if ("jmx.attribute.change".equals(type)) {
      AttributeChangeNotification acn = (AttributeChangeNotification) notification;
      PropertyChangeEvent pce = new PropertyChangeEvent(this, acn.getAttributeName(), acn.getOldValue(), acn
          .getNewValue());
      propertyChangeSupport.firePropertyChange(pce);
    }
  }

  private void fireTunneledBeansRegistered() {
    PropertyChangeEvent pce = new PropertyChangeEvent(this, DSOClientMBean.TUNNELED_BEANS_REGISTERED, null, null);
    propertyChangeSupport.firePropertyChange(pce);
    setReady(true);
  }

  private void initPolledAttributes() {
    registerPolledAttribute(new PolledAttribute(getL1InfoBeanName(), POLLED_ATTR_CPU_USAGE));
    registerPolledAttribute(new PolledAttribute(getL1InfoBeanName(), POLLED_ATTR_USED_MEMORY));
    registerPolledAttribute(new PolledAttribute(getL1InfoBeanName(), POLLED_ATTR_MAX_MEMORY));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_OBJECT_FLUSH_RATE));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_OBJECT_FAULT_RATE));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_TRANSACTION_RATE));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_PENDING_TRANSACTIONS_COUNT));
    registerPolledAttribute(new PolledAttribute(getBeanName(), POLLED_ATTR_LIVE_OBJECT_COUNT));
  }

  @Override
  public synchronized void addPolledAttributeListener(String name, PolledAttributeListener listener) {
    super.addPolledAttributeListener(name, listener);
    clusterModel.addPolledAttributeListener(PollScope.CLIENTS, name, listener);
  }

  private void setReady(boolean ready) {
    boolean oldValue;
    synchronized (this) {
      oldValue = isReady();
      this.ready = ready;
    }
    if (ready != oldValue && ready) {
      initPolledAttributes();
    }
    propertyChangeSupport.firePropertyChange(PROP_READY, oldValue, ready);
  }

  public synchronized boolean isReady() {
    return ready;
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public ObjectName getBeanName() {
    return beanName;
  }

  public ObjectName getL1InfoBeanName() {
    return delegate.getL1InfoBeanName();
  }

  public boolean isTunneledBeansRegistered() {
    return delegate.isTunneledBeansRegistered();
  }

  public long getChannelID() {
    return channelId;
  }

  public ClientID getClientID() {
    return clientId;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public String getHost() {
    if (host == null) {
      host = "unknown";

      String addr = getRemoteAddress();
      if (addr != null && addr.indexOf(':') != -1) {
        host = addr.substring(0, addr.lastIndexOf(':'));
      }
    }

    return host;
  }

  public int getPort() {
    if (port == null) {
      port = Integer.valueOf(-1);

      String addr = getRemoteAddress();
      if (addr != null && addr.indexOf(":") != -1) {
        try {
          port = new Integer(addr.substring(addr.lastIndexOf(':') + 1));
        } catch (Exception e) {/**/
        }
      }
    }

    return port.intValue();
  }

  @Override
  public String toString() {
    return getRemoteAddress();
  }

  public Number[] getDSOStatistics(String[] names) {
    return delegate.getStatistics(names);
  }

  public void addNotificationListener(NotificationListener listener) throws Exception {
    addNotificationListener(beanName, listener);
  }

  public void addNotificationListener(ObjectName on, NotificationListener listener) throws Exception {
    safeRemoveNotificationListener(on, listener);
    cc.addNotificationListener(on, listener);
  }

  public ObjectName getL1InfoObjectName() {
    return delegate.getL1InfoBeanName();
  }

  public L1InfoMBean getL1InfoBean() {
    return l1InfoBean;
  }

  public String[] getCpuStatNames() {
    return getL1InfoBean().getCpuStatNames();
  }

  public StatisticData[] getCpuUsage() {
    return getL1InfoBean().getCpuUsage();
  }

  public long getTransactionRate() {
    return delegate.getTransactionRate();
  }

  public ObjectName getInstrumentationLoggingObjectName() {
    return delegate.getInstrumentationLoggingBeanName();
  }

  public InstrumentationLoggingMBean getInstrumentationLoggingBean() {
    return instrumentationLoggingBean;
  }

  public ObjectName getRuntimeLoggingObjectName() {
    return delegate.getRuntimeLoggingBeanName();
  }

  public RuntimeLoggingMBean getRuntimeLoggingBean() {
    return runtimeLoggingBean;
  }

  public ObjectName getRuntimeOutputOptionsObjectName() {
    return delegate.getRuntimeOutputOptionsBeanName();
  }

  public RuntimeOutputOptionsMBean getRuntimeOutputOptionsBean() {
    return runtimeOutputOptionsBean;
  }

  public String takeThreadDump(long requestMillis) {
    return l1InfoBean != null ? l1InfoBean.takeThreadDump(requestMillis) : "";
  }

  public int getLiveObjectCount() {
    return delegate.getLiveObjectCount();
  }

  public boolean isResident(ObjectID oid) {
    return clusterModel.isResidentOnClient(this, oid);
  }

  public void killClient() {
    delegate.killClient();
  }

  /**
   * TODO: Change this to be like the version in com.tc.admin.model.Server. Remove these "positional parameters" and use
   * string keys.
   */
  public synchronized ProductVersion getProductInfo() {
    if (productInfo == null) {
      String[] attributes = { "Version", "MavenArtifactsVersion", "Patched", "PatchLevel", "PatchVersion", "BuildID",
          "Copyright" };
      String version = ProductInfo.UNKNOWN_VALUE;
      String mavenArtifactsVersion = ProductInfo.UNKNOWN_VALUE;
      String patchLevel = ProductInfo.UNKNOWN_VALUE;
      String patchVersion = ProductInfo.UNKNOWN_VALUE;
      String buildID = ProductInfo.UNKNOWN_VALUE;
      String capabilities = ProductInfo.UNKNOWN_VALUE;
      String copyright = ProductInfo.UNKNOWN_VALUE;
      try {
        AttributeList attrList = cc.mbsc.getAttributes(delegate.getL1InfoBeanName(), attributes);
        if (attrList.get(0) != null) {
          version = (String) ((Attribute) attrList.get(0)).getValue();
        }
        if (attrList.get(1) != null) {
          mavenArtifactsVersion = (String) ((Attribute) attrList.get(1)).getValue();
        }
        boolean isPatched = false;
        if (attrList.get(2) != null) {
          isPatched = (Boolean) ((Attribute) attrList.get(2)).getValue();
        }
        if (attrList.get(3) != null) {
          patchLevel = isPatched ? (String) ((Attribute) attrList.get(3)).getValue() : null;
        }
        if (attrList.get(4) != null) {
          patchVersion = (String) ((Attribute) attrList.get(4)).getValue();
        }
        if (attrList.get(5) != null) {
          buildID = (String) ((Attribute) attrList.get(5)).getValue();
        }
        if (attrList.get(6) != null) {
          copyright = (String) ((Attribute) attrList.get(6)).getValue();
        }
      } catch (Exception e) {
        System.err.println(e);
      }
      productInfo = new ProductVersion(version, mavenArtifactsVersion, patchLevel, patchVersion, buildID, capabilities,
                                       copyright);
    }
    return productInfo;
  }

  public String getProductVersion() {
    return getProductInfo().version();
  }

  public String getProductPatchLevel() {
    return getProductInfo().patchLevel();
  }

  public String getProductPatchVersion() {
    return getProductInfo().patchVersion();
  }

  public String getProductBuildID() {
    return getProductInfo().buildID();
  }

  public String getProductLicense() {
    return getProductInfo().license();
  }

  public String getProductCopyright() {
    return getProductInfo().copyright();
  }

  public String getConfig() {
    return getL1InfoBean().getConfig();
  }

  public String getEnvironment() {
    return getL1InfoBean().getEnvironment();
  }

  public Map getL1Statistics() {
    return getL1InfoBean().getStatistics();
  }

  /**
   * Cpu usage, Memory usage, Transaction rate.
   * 
   * @see IClusterModel.getPrimaryClientStatistics
   * @see IClusterModel.getPrimaryServerStatistics
   */
  public Map getPrimaryStatistics() {
    Map result = getL1Statistics();
    result.put("TransactionRate", getTransactionRate());
    return result;
  }

  public String dump() {
    StringBuilder sb = new StringBuilder(toString());
    sb.append(" ready: ");
    sb.append(isReady());
    sb.append(" isConnected: ");
    sb.append(cc.isConnected());
    return sb.toString();
  }

  @Override
  public void tearDown() {
    if (!isReady()) {
      stopListeningForTunneledBeans();
    }
    super.tearDown();
  }

  public void gc() {
    getL1InfoBean().gc();
  }

  public boolean isVerboseGC() {
    return getL1InfoBean().isVerboseGC();
  }

  public void setVerboseGC(boolean verboseGC) {
    getL1InfoBean().setVerboseGC(verboseGC);
  }
}
