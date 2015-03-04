package com.tc.admin;

import org.junit.Test;

import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.object.config.schema.L2DSOConfig;
import com.terracottatech.config.BindPort;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TCStopTest {

  @Test
  public void computeJMXPortDefined() throws ConfigurationSetupException {
    int jmxPort = 9525;

    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);

    when(l2Config.jmxPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(jmxPort);

    int jmxPortResult = TCStop.computeJMXPort(null, l2Config, null);
    assertThat(jmxPortResult, is(jmxPort));
  }

  @Test
  public void computeJMXPortDefinedAsZero() throws ConfigurationSetupException {
    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);

    when(l2Config.jmxPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(0);

    int jmxPortResult = TCStop.computeJMXPort(null, l2Config, null);
    assertThat(jmxPortResult, is(TCStop.DEFAULT_PORT));
  }

  @Test
  public void computeJMXPortUndefined() throws ConfigurationSetupException {
    int tsaPort = 9515;
    String name = "server";

    CommonL2Config l2Config = mock(CommonL2Config.class);
    BindPort bindPort = mock(BindPort.class);
    L2ConfigurationSetupManager manager = mock(L2ConfigurationSetupManager.class);
    L2DSOConfig dsoConfig = mock(L2DSOConfig.class);

    when(manager.dsoL2ConfigFor(name)).thenReturn(dsoConfig);
    when(dsoConfig.dsoPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(tsaPort);

    int jmxPortResult = TCStop.computeJMXPort(manager, l2Config, name);
    assertThat(jmxPortResult, is(tsaPort + CommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT));
  }

}