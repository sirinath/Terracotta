package com.tc.server.util;

import org.junit.Test;

import com.terracottatech.config.BindPort;
import com.terracottatech.config.Server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerStatTest {

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