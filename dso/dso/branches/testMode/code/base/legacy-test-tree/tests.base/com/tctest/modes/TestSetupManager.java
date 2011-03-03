/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.modes;

public abstract class TestSetupManager {
  private boolean canRunL1ProxyConnect            = false;
  private boolean canSkipL1ReconnectCheck         = false;
  private boolean enableManualProxyConnectControl = false;
  private boolean enableL1Reconnect               = false;
  private boolean canRunL2ProxyConnect            = false;
  private boolean enableL2Reconnect               = false;

  public TestSetupManager setCanRunL1ProxyConnect(boolean canRunL1ProxyConnect) {
    this.canRunL1ProxyConnect = canRunL1ProxyConnect;
    return this;
  }

  public TestSetupManager setCanSkipL1ReconnectCheck(boolean canSkipL1ReconnectCheck) {
    this.canSkipL1ReconnectCheck = canSkipL1ReconnectCheck;
    return this;
  }

  public TestSetupManager setEnableManualProxyConnectControl(boolean enableManualProxyConnectControl) {
    this.enableManualProxyConnectControl = enableManualProxyConnectControl;
    return this;
  }

  public TestSetupManager setEnableL1Reconnect(boolean enableL1Reconnect) {
    this.enableL1Reconnect = enableL1Reconnect;
    return this;
  }

  public TestSetupManager setCanRunL2ProxyConnect(boolean canRunL2ProxyConnect) {
    this.canRunL2ProxyConnect = canRunL2ProxyConnect;
    return this;
  }

  public TestSetupManager setEnableL2Reconnect(boolean enableL2Reconnect) {
    this.enableL2Reconnect = enableL2Reconnect;
    return this;
  }

  public boolean canRunL1ProxyConnect() {
    return canRunL1ProxyConnect;
  }

  public boolean canSkipL1ReconnectCheck() {
    return canSkipL1ReconnectCheck;
  }

  public boolean enableManualProxyConnectControl() {
    return enableManualProxyConnectControl;
  }

  public boolean enableL1Reconnect() {
    return enableL1Reconnect;
  }

  public boolean canRunL2ProxyConnect() {
    return canRunL2ProxyConnect;
  }

  public boolean enableL2Reconnect() {
    return enableL2Reconnect;
  }
}
