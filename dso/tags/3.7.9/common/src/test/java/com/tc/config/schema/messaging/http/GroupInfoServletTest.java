/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.terracotta.groupConfigForL1.ServerGroupsDocument;
import org.terracotta.groupConfigForL1.ServerInfo;

import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.setup.ConfigurationCreator;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.ConfigurationSpec;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManagerImpl;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.StandardXMLFileConfigurationCreator;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import javax.servlet.http.HttpServletResponse;

public class GroupInfoServletTest extends TCTestCase {
  L2ConfigurationSetupManager configSetupMgr;
  FileOutputStream            out;

  private class GroupInfoServletForTest extends GroupInfoServlet {
    @Override
    protected L2ConfigurationSetupManager getConfigurationManager() {
      return configSetupMgr;
    }

    @Override
    protected OutputStream getOutPutStream(HttpServletResponse response1) throws IOException {
      out = new FileOutputStream(getTempFile("temp.xml"));
      return out;
    }
  }

  private File           tcConfig = null;
  @Mock
  HttpServletResponse    response;
  private BufferedReader bufferedReader;

  @Override
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Override
  protected File getTempFile(String fileName) throws IOException {
    return getTempDirectoryHelper().getFile(fileName);
  }

  private synchronized void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out1 = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out1);
      out1.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public void testCreateServerNameTodsoPortAndHostname() throws IOException, ConfigurationSetupException {
    tcConfig = getTempFile("bind-address.xml");
    String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\"\n\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n\txsi:schemaLocation=\"http://www.terracotta.org/schema/terracotta-6.xsd\">\n\n\t<servers>\n\t\t<server host=\"10.60.52.156\" name=\"S119D90\" bind=\"10.60.54.69\">\n\t\t\t<dso-port bind=\"10.60.52.156\">9610</dso-port>\n\t\t\t<jmx-port bind=\"10.60.54.69\">9620</jmx-port>\n\t\t\t<l2-group-port bind=\"10.60.52.156\">9630</l2-group-port>\n\t\t\t<data>S119D90-data</data>\n\t\t\t<logs>S119D90-logs</logs>\n\t\t\t<statistics>S119D90-stats</statistics>\n\t\t\t<dso>\n\t\t\t\t<persistence>\n\t\t\t\t\t<mode>permanent-store</mode>\n\t\t\t\t</persistence>\n\t\t\t</dso>\n\t\t</server>\n\n\t\t<server host=\"10.60.52.156\" name=\"S119D91\" bind=\"10.60.54.69\">\n\t\t\t<dso-port bind=\"10.60.52.156\">9640</dso-port>\n\t\t\t<jmx-port bind=\"10.60.54.69\">9650</jmx-port>\n\t\t\t<l2-group-port bind=\"10.60.52.156\">9660</l2-group-port>\n\t\t\t<data>S119D91-data</data>\n\t\t\t<logs>S119D91-logs</logs>\n\t\t\t<statistics>S119D91-stats</statistics>\n\t\t\t<dso>\n\t\t\t\t<persistence>\n\t\t\t\t\t<mode>permanent-store</mode>\n\t\t\t\t</persistence>\n\t\t\t</dso>\n\t\t</server>\n\t\t\n\t\t<mirror-groups>\n\t\t\t<mirror-group>\n\t\t\t\t<members>\n\t\t\t\t\t<member>S119D90</member>\n\t\t\t\t\t<member>S119D91</member>\n\t\t\t\t</members>\n\t\t\t\t<ha>\n\t\t\t\t\t<mode>networked-active-passive</mode>\n\t\t\t\t\t<networked-active-passive>\n\t\t\t\t\t\t<election-time>5</election-time>\n\t\t\t\t\t</networked-active-passive>\n\t\t\t\t</ha>\n\t\t\t</mirror-group>\n\t\t</mirror-groups>\n\n\t\t<update-check>\n\t\t\t<enabled>true</enabled>\n\t\t</update-check>\n\n\t</servers>\n\n\t<clients>\n\t\t<logs>client-logs</logs>\n\t</clients>\n</tc:tc-config>\n";
    writeConfigFile(config);
    configSetupMgr = initializeAndGetL2ConfigurationSetupManager();
    GroupInfoServletForTest groupInfoServlet = new GroupInfoServletForTest();
    groupInfoServlet.doGet(null, response);
    String str;
    bufferedReader = new BufferedReader(new FileReader(getTempFile("temp.xml")));
    bufferedReader.readLine();
    str = bufferedReader.readLine();
    ServerGroupsDocument parse = null;
    try {
      parse = ServerGroupsDocument.Factory.parse(str);
    } catch (XmlException e) {
      Assert.fail();
    }
    ServerInfo[] serverInfoArray = parse.getServerGroups().getServerGroupArray()[0].getServerInfoArray();
    Assert.assertEquals("10.60.52.156", serverInfoArray[0].getName());
    Assert.assertEquals(BigInteger.valueOf(9610), serverInfoArray[0].getDsoPort());
    Assert.assertEquals("10.60.52.156", serverInfoArray[1].getName());
    Assert.assertEquals(BigInteger.valueOf(9640), serverInfoArray[1].getDsoPort());

  }

  private L2ConfigurationSetupManager initializeAndGetL2ConfigurationSetupManager() throws IOException,
      ConfigurationSetupException {
    File cwd = getTempDirectory();

    ConfigurationSpec configurationSpec = new ConfigurationSpec(tcConfig.getAbsolutePath(), null,
                                                                StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                cwd);

    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(
                                                                                        configurationSpec,
                                                                                        new TerracottaDomainConfigurationDocumentBeanFactory());

    configSetupMgr = new L2ConfigurationSetupManagerImpl(configurationCreator, "S119D90",
                                                         new SchemaDefaultValueProvider(),
                                                         new StandardXmlObjectComparator(),
                                                         new FatalIllegalConfigurationChangeHandler());

    return configSetupMgr;
  }
}
