package com.tc.server.util;

import org.junit.Test;

import com.tc.config.schema.CommonL2Config;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.Server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerStatTest {

  @Test
  public void computeJMXPortDefined() {
    int jmxPort = 9525;

    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.isSetJmxPort()).thenReturn(true);
    when(server.getJmxPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(jmxPort);

    int jmxPortResult = ServerStat.computeJMXPort(server);
    assertThat(jmxPortResult, is(jmxPort));
  }

  @Test
  public void computeJMXPortDefinedAsZero() {
    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.isSetJmxPort()).thenReturn(true);
    when(server.getJmxPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(0);

    int jmxPortResult = ServerStat.computeJMXPort(server);
    assertThat(jmxPortResult, is(ServerStat.DEFAULT_JMX_PORT));
  }

  @Test
  public void computeJMXPortUndefined() {
    int tsaPort = 9525;

    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.isSetJmxPort()).thenReturn(false);
    when(server.getDsoPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(tsaPort);

    int jmxPortResult = ServerStat.computeJMXPort(server);
    assertThat(jmxPortResult, is(tsaPort + CommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT));
  }

}