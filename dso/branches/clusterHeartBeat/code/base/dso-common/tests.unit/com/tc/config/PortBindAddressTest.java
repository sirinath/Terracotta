/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class PortBindAddressTest extends TCTestCase {
  private File tcConfig = null;

  public void testBindPort() {
    try {
      tcConfig = getTempFile("tc-config-testHaMode1.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">" 
                      + "\n      <dso-port bind=\"127.8.9.0\">6510</dso-port>"
                      + "\n      <jmx-port bind=\"127.8.9.1\">6520</jmx-port>"
                      + "\n      <l2-group-port bind=\"127.8.9.2\">6530</l2-group-port>"  
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <server name=\"server2\">"
                      + "\n      <dso-port>8510</dso-port>"
                      + "\n      <jmx-port>8520</jmx-port>"
                      + "\n      <l2-group-port>8530</l2-group-port>"
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <server name=\"server3\" bind=\"1.2.3.4\">" 
                      + "\n      <dso-port bind=\"127.8.9.0\">7510</dso-port>"
                      + "\n      <jmx-port bind=\"127.8.9.1\">7520</jmx-port>"
                      + "\n      <l2-group-port bind=\"127.8.9.2\">7530</l2-group-port>"  
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>"
                      + "\n      <server name=\"server4\" bind=\"1.2.3.4\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>"
                      + "\n</servers>" 
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals("127.8.9.0", configSetupMgr.dsoL2Config().dsoPort().getBindAddress());
      Assert.assertEquals("127.8.9.1", configSetupMgr.commonl2Config().jmxPort().getBindAddress());
      Assert.assertEquals("127.8.9.2", configSetupMgr.dsoL2Config().l2GroupPort().getBindAddress());
      Assert.assertEquals(6510, configSetupMgr.dsoL2Config().dsoPort().getBindPort());
      Assert.assertEquals(6530, configSetupMgr.dsoL2Config().l2GroupPort().getBindPort());
      Assert.assertEquals(6520, configSetupMgr.commonl2Config().jmxPort().getBindPort());
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().dsoPort().getBindAddress());
      Assert.assertEquals("0.0.0.0", configSetupMgr.commonl2Config().jmxPort().getBindAddress());
      Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().l2GroupPort().getBindAddress());
      Assert.assertEquals(8510, configSetupMgr.dsoL2Config().dsoPort().getBindPort());
      Assert.assertEquals(8530, configSetupMgr.dsoL2Config().l2GroupPort().getBindPort());
      Assert.assertEquals(8520, configSetupMgr.commonl2Config().jmxPort().getBindPort());
     
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server3");
      Assert.assertEquals("127.8.9.0", configSetupMgr.dsoL2Config().dsoPort().getBindAddress());
      Assert.assertEquals("127.8.9.1", configSetupMgr.commonl2Config().jmxPort().getBindAddress());
      Assert.assertEquals("127.8.9.2", configSetupMgr.dsoL2Config().l2GroupPort().getBindAddress());
      Assert.assertEquals(7510, configSetupMgr.dsoL2Config().dsoPort().getBindPort());
      Assert.assertEquals(7530, configSetupMgr.dsoL2Config().l2GroupPort().getBindPort());
      Assert.assertEquals(7520, configSetupMgr.commonl2Config().jmxPort().getBindPort());
      
      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server4");
      Assert.assertEquals("1.2.3.4", configSetupMgr.dsoL2Config().dsoPort().getBindAddress());
      Assert.assertEquals("1.2.3.4", configSetupMgr.commonl2Config().jmxPort().getBindAddress());
      Assert.assertEquals("1.2.3.4", configSetupMgr.dsoL2Config().l2GroupPort().getBindAddress());
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().dsoPort().getBindPort());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().l2GroupPort().getBindPort());
      Assert.assertEquals(9520, configSetupMgr.commonl2Config().jmxPort().getBindPort());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private synchronized void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

}
