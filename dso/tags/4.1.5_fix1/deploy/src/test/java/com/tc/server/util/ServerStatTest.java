package com.tc.server.util;

import org.junit.Test;

import com.tc.object.config.schema.L2DSOConfigObject;
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
    when(server.isSetTsaPort()).thenReturn(true);
    when(server.getTsaPort()).thenReturn(bindPort);
    when(bindPort.getIntValue()).thenReturn(tsaPort);

    int jmxPortResult = ServerStat.computeJMXPort(server);
    assertThat(jmxPortResult, is(tsaPort + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT));
  }

  @Test
  public void computeJMXPortUndefinedTSAPortAlsoUndefined() {
    Server server = mock(Server.class);

    when(server.isSetJmxPort()).thenReturn(false);
    when(server.isSetTsaPort()).thenReturn(false);

    int jmxPortResult = ServerStat.computeJMXPort(server);
    assertThat(jmxPortResult, is(ServerStat.DEFAULT_TSA_PORT + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT));
  }

  @Test
  public void testComputeTargetHostWithoutJmxPort() {
    String expectedHost = "mock.tc.org";

    Server server = mock(Server.class);

    when(server.getHost()).thenReturn(expectedHost);

    String host = ServerStat.computeTargetHost(server);
    assertThat(host, is(expectedHost));
  }

  @Test
  public void testComputeTargetHostWithoutJmxPortBinding() {
    String expectedHost = "mock.tc.org";

    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.getHost()).thenReturn(expectedHost);
    when(server.getJmxPort()).thenReturn(bindPort);

    String host = ServerStat.computeTargetHost(server);
    assertThat(host, is(expectedHost));
  }

  @Test
  public void testComputeTargetHostWithJmxPortBinding() {
    String expectedHost = "mock.tc.org";

    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.getJmxPort()).thenReturn(bindPort);
    when(bindPort.getBind()).thenReturn(expectedHost);

    String host = ServerStat.computeTargetHost(server);
    assertThat(host, is(expectedHost));
  }

  @Test
  public void testComputeTargetHostWithInvalidIPv4JmxPortBinding() {
    String expectedHost = "mock.tc.org";
    String jmxHostBinding = "0.0.0.0";

    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.isSetJmxPort()).thenReturn(true);
    when(server.getHost()).thenReturn(expectedHost);
    when(server.getJmxPort()).thenReturn(bindPort);
    when(bindPort.getBind()).thenReturn(jmxHostBinding);

    String host = ServerStat.computeTargetHost(server);
    assertThat(host, is(expectedHost));
  }

  @Test
  public void testComputeTargetHostWithInvalidIPv6JmxPortBinding() {
    String expectedHost = "mock.tc.org";
    String jmxHostBinding = "::";

    Server server = mock(Server.class);
    BindPort bindPort = mock(BindPort.class);

    when(server.isSetJmxPort()).thenReturn(true);
    when(server.getHost()).thenReturn(expectedHost);
    when(server.getJmxPort()).thenReturn(bindPort);
    when(bindPort.getBind()).thenReturn(jmxHostBinding);

    String host = ServerStat.computeTargetHost(server);
    assertThat(host, is(expectedHost));
  }

  @Test
  public void testComputeTargetHostWithoutJmxPortBindingAndServerHost() {
    String expectedHost = "localhost";

    Server server = mock(Server.class);

    String host = ServerStat.computeTargetHost(server);
    assertThat(host, is(expectedHost));
  }

}