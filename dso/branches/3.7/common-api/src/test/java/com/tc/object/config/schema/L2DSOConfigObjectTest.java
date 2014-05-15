package com.tc.object.config.schema;

import org.junit.Test;

import com.tc.config.schema.CommonL2Config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class L2DSOConfigObjectTest {

  @Test
  public void computeJMXPortFromTSAPortDefault() {
    int tsaPort = 9510;
    assertThat(L2DSOConfigObject.computeJMXPortFromTSAPort(tsaPort), is(tsaPort + CommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT));
  }

  @Test
  public void computeJMXPortFromTSAPortAboveMaximumPort() {
    int tsaPort = L2DSOConfigObject.MAX_PORTNUMBER - 1;
    int jmxPort = L2DSOConfigObject.computeJMXPortFromTSAPort(tsaPort);
    assertTrue(jmxPort >= L2DSOConfigObject.MIN_PORTNUMBER);
    assertTrue(jmxPort <= L2DSOConfigObject.MAX_PORTNUMBER);
  }

}