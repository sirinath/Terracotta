package com.tc.object.config.schema;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class L2DSOConfigObjectTest {

  @Test
  public void computeJMXPortFromTSAPortDefault() {
    int tsaPort = 9510;
    assertThat(L2DSOConfigObject.computeJMXPortFromTSAPort(tsaPort), is(tsaPort + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT));
  }

  @Test
  public void computeJMXPortFromTSAPortAboveMaximumPort() {
    int tsaPort = L2DSOConfigObject.MAX_PORTNUMBER - 1;
    int jmxPort = L2DSOConfigObject.computeJMXPortFromTSAPort(tsaPort);
    assertTrue(jmxPort >= L2DSOConfigObject.MIN_PORTNUMBER);
    assertTrue(jmxPort <= L2DSOConfigObject.MAX_PORTNUMBER);
  }

}