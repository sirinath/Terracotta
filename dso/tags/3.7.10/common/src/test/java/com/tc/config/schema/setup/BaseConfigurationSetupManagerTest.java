/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory.ConfigMode;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.runtime.Os;
import com.terracottatech.config.Application;
import com.terracottatech.config.Client;
import com.terracottatech.config.ConfigurationModel;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.DsoClientDebugging;
import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.Ha;
import com.terracottatech.config.HaMode;
import com.terracottatech.config.InstrumentationLogging;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.Persistence;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.RuntimeLogging;
import com.terracottatech.config.RuntimeOutputOptions;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

public class BaseConfigurationSetupManagerTest extends TCTestCase {

  private static final String DEFAULT_CONFIG_SPEC       = "tc-config.xml";
  private static final String CONFIG_SPEC_ARGUMENT_NAME = "config";
  private static final String CONFIG_FILE_PROPERTY_NAME = "tc.config";
  private static final String DEFAULT_CONFIG_PATH       = "default-config.xml";
  private static final String DEFAULT_CONFIG_URI        = "resource:///"
                                                          + BaseConfigurationSetupManagerTest.class.getPackage()
                                                              .getName().replace('.', '/') + "/" + DEFAULT_CONFIG_PATH;
  private File                tcConfig                  = null;

  public void testServerDefaults1() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    + "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    Assert.assertEquals(9510, server.getDsoPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getDsoPort().getBind());

    int tempGroupPort = 9510 + CommonL2Config.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
    int defaultGroupPort = ((tempGroupPort <= CommonL2Config.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % CommonL2Config.MAX_PORTNUMBER) + CommonL2Config.MIN_PORTNUMBER);

    int tempJmxPort = 9510 + CommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
    int defaultJmxPort = ((tempJmxPort <= CommonL2Config.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % CommonL2Config.MAX_PORTNUMBER) + CommonL2Config.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getL2GroupPort().getBind());

  }

  public void testServerDefaults2() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    +     "<dso-port>8513</dso-port>" 
                    +   "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(),
                        server.getName());

    int dsoPort = 8513;

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getDsoPort().getBind());

    int tempGroupPort = dsoPort + CommonL2Config.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
    int defaultGroupPort = ((tempGroupPort <= CommonL2Config.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % CommonL2Config.MAX_PORTNUMBER) + CommonL2Config.MIN_PORTNUMBER);

    int tempJmxPort = dsoPort + CommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
    int defaultJmxPort = ((tempJmxPort <= CommonL2Config.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % CommonL2Config.MAX_PORTNUMBER) + CommonL2Config.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getL2GroupPort().getBind());

  }

  public void testServerDefaults3() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    +     "<dso-port bind=\"1.2.3.4\">8513</dso-port>" 
                    +   "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    int dsoPort = 8513;
    String dsoBind = "1.2.3.4";

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(dsoBind, server.getDsoPort().getBind());

    int tempGroupPort = dsoPort + CommonL2Config.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
    int defaultGroupPort = ((tempGroupPort <= CommonL2Config.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % CommonL2Config.MAX_PORTNUMBER) + CommonL2Config.MIN_PORTNUMBER);

    int tempJmxPort = dsoPort + CommonL2Config.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
    int defaultJmxPort = ((tempJmxPort <= CommonL2Config.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % CommonL2Config.MAX_PORTNUMBER) + CommonL2Config.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getL2GroupPort().getBind());

  }

  public void testServerDefaults4() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    +     "<dso-port bind=\"1.2.3.4\">8513</dso-port>" 
                    +     "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    +     "<l2-group-port bind=\"5.6.7.8\">7513</l2-group-port>" 
                    +   "</server>" 
                    + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    int dsoPort = 8513;
    String dsoBind = "1.2.3.4";

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(dsoBind, server.getDsoPort().getBind());

    int jmxPort = 9513;
    String jmxBind = "4.3.2.1";
    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    int l2GroupPort = 7513;
    String l2GroupBind = "5.6.7.8";
    Assert.assertEquals(l2GroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(l2GroupBind, server.getL2GroupPort().getBind());
  }

  public void testServerDefaults5() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    +     "<dso-port bind=\"1.2.3.4\">8513</dso-port>" 
                    +     "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    +     "<l2-group-port bind=\"5.6.7.8\">7513</l2-group-port>" 
                    +   "</server>"
                    +   "<server host=\"testHost2\" name=\"server2\" bind=\"4.5.6.7\">"
                    +     "<dso-port bind=\"1.2.3.4\">8513</dso-port>" 
                    +     "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    +     "<l2-group-port bind=\"5.6.7.8\">7513</l2-group-port>" 
                    +   "</server>" 
                    + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(2, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    int dsoPort = 8513;
    String dsoBind = "1.2.3.4";

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(dsoBind, server.getDsoPort().getBind());

    int jmxPort = 9513;
    String jmxBind = "4.3.2.1";
    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    int l2GroupPort = 7513;
    String l2GroupBind = "5.6.7.8";
    Assert.assertEquals(l2GroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(l2GroupBind, server.getL2GroupPort().getBind());

    server = servers.getServerArray(1);
    String host = "testHost2";
    String name = "server2";
    String bind = "4.5.6.7";

    Assert.assertEquals(host, server.getHost());
    Assert.assertEquals(bind, server.getBind());
    Assert.assertEquals(name, server.getName());

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(dsoBind, server.getDsoPort().getBind());

    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    Assert.assertEquals(l2GroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(l2GroupBind, server.getL2GroupPort().getBind());

  }

  public void testServerDiretctoryDefaults() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    + " </server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "data")
        .getAbsolutePath(), server.getData());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "logs")
        .getAbsolutePath(), server.getLogs());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + "data-backup").getAbsolutePath(), server.getDataBackup());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + "statistics").getAbsolutePath(), server.getStatistics());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "data"
                                 + File.separator + "index").getAbsolutePath(), server.getIndex());
  }

  public void testServerDiretctoryPaths() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    +     "<data>abc/xyz/123</data>" 
                    +     "<logs>xyz/abc/451</logs>"
                    +     "<data-backup>/qrt/opt/pqr</data-backup>" 
                    +     "<statistics>/opq/pqr/123/or</statistics>"
                    +     "<index>/rta/try/456</index>" 
                    +   "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert
        .assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "abc"
                               + File.separator + "xyz" + File.separator + "123").getAbsolutePath(), server.getData());
    Assert
        .assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "xyz"
                               + File.separator + "abc" + File.separator + "451").getAbsolutePath(), server.getLogs());
    if (Os.isWindows()) {
      // for windows box
      Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "qrt"
                                   + File.separator + "opt" + File.separator + "pqr").getAbsolutePath(), server
          .getDataBackup());
      Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "opq"
                                   + File.separator + "pqr" + File.separator + "123" + File.separator + "or")
          .getAbsolutePath(), server.getStatistics());
      Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "rta"
                                   + File.separator + "try" + File.separator + "456").getAbsolutePath(), server
          .getIndex());
    } else {
      Assert.assertEquals("/qrt/opt/pqr", server.getDataBackup());
      Assert.assertEquals("/opq/pqr/123/or", server.getStatistics());
      Assert.assertEquals("/rta/try/456", server.getIndex());
    }
  }

  public void testServerSubsitutedDirtctoryPaths() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    +     "<data>%h</data>" 
                    +     "<logs>%i</logs>" 
                    +     "<data-backup>%H</data-backup>"
                    +     "<statistics>%n</statistics>" 
                    +   "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + InetAddress.getLocalHost().getHostName()).getAbsolutePath(), server.getData());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + InetAddress.getLocalHost().getHostAddress()).getAbsolutePath(), server.getLogs());
    Assert.assertEquals(System.getProperty("user.home"), server.getDataBackup());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + System.getProperty("user.name")).getAbsolutePath(), server.getStatistics());
  }

  public void testDefaultDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    +   "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(PersistenceMode.TEMPORARY_SWAP_ONLY, server.getDso().getPersistence().getMode());
    Assert.assertEquals(120, server.getDso().getClientReconnectWindow());
    Assert.assertEquals(true, server.getDso().getGarbageCollection().getEnabled());
    Assert.assertEquals(false, server.getDso().getGarbageCollection().getVerbose());
    Assert.assertEquals(3600, server.getDso().getGarbageCollection().getInterval());
  }

  public void testDefaultPersistence() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);
    Assert.assertTrue(server.isSetDso());
    DsoServerData dsoServerData = server.getDso();
    Assert.assertTrue(dsoServerData.isSetPersistence());
    Assert.assertEquals(PersistenceMode.TEMPORARY_SWAP_ONLY, dsoServerData.getPersistence().getMode());
    Persistence persistence = dsoServerData.getPersistence();
    Assert.assertTrue(persistence.isSetMode());
    Assert.assertFalse(persistence.isSetOffheap());
  }

  public void testDefaultOffHeap() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>" 
                    +     "<dso>"
                    +       "<persistence>" 
                    +         "<mode>permanent-store</mode>" 
                    +       "</persistence>"
                    +       "<client-reconnect-window>9876</client-reconnect-window>" 
                    +       "<garbage-collection>"
                    +         "<enabled>false</enabled>" 
                    +         "<verbose>true</verbose>" 
                    +         "<interval>1234</interval>"
                    +         "</garbage-collection>" 
                    +     "</dso>" 
                    +   "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(PersistenceMode.PERMANENT_STORE, server.getDso().getPersistence().getMode());
    Assert.assertEquals(9876, server.getDso().getClientReconnectWindow());
    Assert.assertEquals(false, server.getDso().getGarbageCollection().getEnabled());
    Assert.assertEquals(true, server.getDso().getGarbageCollection().getVerbose());
    Assert.assertEquals(1234, server.getDso().getGarbageCollection().getInterval());

    Assert.assertFalse(server.getDso().getPersistence().isSetOffheap());
  }

  public void testOffHeap1() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>" 
                    +     "<dso>"
                    +       "<persistence>" 
                    +         "<mode>permanent-store</mode>" 
                    +         "<offheap>" 
                    +           "<enabled>true</enabled>"
                    +           "<maxDataSize>5628m</maxDataSize>" 
                    +         "</offheap>" 
                    +       "</persistence>"
                    +       "<client-reconnect-window>9876</client-reconnect-window>" 
                    +       "<garbage-collection>"
                    +         "<enabled>false</enabled>" 
                    +         "<verbose>true</verbose>" 
                    +         "<interval>1234</interval>"
                    +       "</garbage-collection>" 
                    +     "</dso>" 
                    +   "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(PersistenceMode.PERMANENT_STORE, server.getDso().getPersistence().getMode());
    Assert.assertEquals(9876, server.getDso().getClientReconnectWindow());
    Assert.assertEquals(false, server.getDso().getGarbageCollection().getEnabled());
    Assert.assertEquals(true, server.getDso().getGarbageCollection().getVerbose());
    Assert.assertEquals(1234, server.getDso().getGarbageCollection().getInterval());

    Assert.assertTrue(server.getDso().getPersistence().isSetOffheap());
    Assert.assertEquals(true, server.getDso().getPersistence().getOffheap().getEnabled());
    Assert.assertEquals("5628m", server.getDso().getPersistence().getOffheap().getMaxDataSize());
  }

  public void testOffHeap2() throws IOException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>" 
                    +     "<dso>"
                    +       "<persistence>" 
                    +         "<mode>permanent-store</mode>" 
                    +         "<offheap>" 
                    +           "<enabled>true</enabled>"
                    +         "</offheap>" 
                    +       "</persistence>" 
                    +       "<client-reconnect-window>9876</client-reconnect-window>"
                    +       "<garbage-collection>" 
                    +         "<enabled>false</enabled>" 
                    +         "<verbose>true</verbose>"
                    +         "<interval>1234</interval>" 
                    +       "</garbage-collection>" 
                    +     "</dso>" 
                    +   "</server>" 
                    + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    try {
      initializeAndGetBaseTVSConfigSetupManager(false);
      Assert.fail("parsing should fail since maxDataSize was missing");

    } catch (ConfigurationSetupException e) {
      // do noting expected exception
    }
  }

  public void testDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>" 
                    +     "<dso>"
                    +       "<persistence>" 
                    +         "<mode>permanent-store</mode>" 
                    +       "</persistence>"
                    +       "<client-reconnect-window>9876</client-reconnect-window>" 
                    +       "<garbage-collection>"
                    +         "<enabled>false</enabled>" 
                    +         "<verbose>true</verbose>" 
                    +         "<interval>1234</interval>"
                    +       "</garbage-collection>" 
                    +     "</dso>" 
                    +   "</server>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(PersistenceMode.PERMANENT_STORE, server.getDso().getPersistence().getMode());
    Assert.assertEquals(9876, server.getDso().getClientReconnectWindow());
    Assert.assertEquals(false, server.getDso().getGarbageCollection().getEnabled());
    Assert.assertEquals(true, server.getDso().getGarbageCollection().getVerbose());
    Assert.assertEquals(1234, server.getDso().getGarbageCollection().getInterval());
  }

  public void testMirrorGroupDefaults() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Assert.assertTrue(servers.isSetMirrorGroups());
    MirrorGroups mirrorGroups = servers.getMirrorGroups();
    Assert.assertEquals(1, mirrorGroups.sizeOfMirrorGroupArray());
    Assert.assertEquals(1, mirrorGroups.getMirrorGroupArray().length);

    Server server = servers.getServerArray(0);
    MirrorGroup mirrorGroup = mirrorGroups.getMirrorGroupArray(0);
    Assert.assertEquals(1, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(1, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(server.getName(), mirrorGroup.getMembers().getMemberArray(0));

    Assert.assertTrue(mirrorGroup.isSetHa());
    Ha defaultHa = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, defaultHa.getMode());
    Assert.assertEquals(5, defaultHa.getNetworkedActivePassive().getElectionTime());
  }

  public void testMirrorGroupWithDefaultHa() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>"
                    +   "<server host=\"eng01\" name=\"server1\"></server>"
                    +   "<server host=\"eng02\" name=\"server2\"></server>"
                    +   "<server host=\"eng03\" name=\"server3\"></server>"
                    +   "<server host=\"eng04\" name=\"server4\"></server>" 
                    +   "<mirror-groups>"
                    +     "<mirror-group group-name=\"group1\">" 
                    +     "<members>" 
                    +       "<member>server1</member>"
                    +       "<member>server2</member>" 
                    +     "</members>" 
                    +   "</mirror-group>"
                    +   "<mirror-group group-name=\"group2\">" 
                    +     "<members>" 
                    +       "<member>server3</member>"
                    +       "<member>server4</member>" 
                    +     "</members>" 
                    +   "</mirror-group>" 
                    +   "</mirror-groups>" 
                    + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(4, servers.getServerArray().length);
    Assert.assertTrue(servers.isSetMirrorGroups());
    MirrorGroups mirrorGroups = servers.getMirrorGroups();
    Assert.assertEquals(2, mirrorGroups.sizeOfMirrorGroupArray());
    Assert.assertEquals(2, mirrorGroups.getMirrorGroupArray().length);

    MirrorGroup mirrorGroup = mirrorGroups.getMirrorGroupArray(0);
    Assert.assertEquals(2, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(2, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(servers.getServerArray(0).getName(), mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals("server1", mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals(servers.getServerArray(1).getName(), mirrorGroup.getMembers().getMemberArray(1));
    Assert.assertEquals("server2", mirrorGroup.getMembers().getMemberArray(1));

    Assert.assertTrue(mirrorGroup.isSetHa());
    Ha defaultHa = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, defaultHa.getMode());
    Assert.assertEquals(5, defaultHa.getNetworkedActivePassive().getElectionTime());

    mirrorGroup = mirrorGroups.getMirrorGroupArray(1);
    Assert.assertEquals(2, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(2, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(servers.getServerArray(2).getName(), mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals("server3", mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals(servers.getServerArray(3).getName(), mirrorGroup.getMembers().getMemberArray(1));
    Assert.assertEquals("server4", mirrorGroup.getMembers().getMemberArray(1));

    Assert.assertTrue(mirrorGroup.isSetHa());
    defaultHa = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, defaultHa.getMode());
    Assert.assertEquals(5, defaultHa.getNetworkedActivePassive().getElectionTime());
  }

  public void testMirrorGroupWithGivenHa() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>"
                    +   "<server host=\"eng01\" name=\"server1\"></server>"
                    +   "<server host=\"eng02\" name=\"server2\"></server>"
                    +   "<server host=\"eng03\" name=\"server3\"></server>"
                    +   "<server host=\"eng04\" name=\"server4\"></server>" 
                    +   "<mirror-groups>"
                    +     "<mirror-group group-name=\"group1\">" 
                    +       "<members>" 
                    +         "<member>server1</member>"
                    +         "<member>server2</member>" 
                    +       "</members>" 
                    +     "</mirror-group>"
                    +     "<mirror-group group-name=\"group2\">" 
                    +       "<members>" 
                    +         "<member>server3</member>"
                    +         "<member>server4</member>" 
                    +       "</members>" 
                    +       "<ha>" 
                    +         "<mode>networked-active-passive</mode>"
                    +           "<networked-active-passive>" 
                    +             "<election-time>15</election-time>"
                    +           "</networked-active-passive>" 
                    +       "</ha>" 
                    +       "</mirror-group>" 
                    +     "</mirror-groups>" 
                    +   "<ha>"
                    +     "<mode>disk-based-active-passive</mode>" 
                    +   "</ha>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(4, servers.getServerArray().length);
    Assert.assertTrue(servers.isSetMirrorGroups());
    MirrorGroups mirrorGroups = servers.getMirrorGroups();
    Assert.assertEquals(2, mirrorGroups.sizeOfMirrorGroupArray());
    Assert.assertEquals(2, mirrorGroups.getMirrorGroupArray().length);

    MirrorGroup mirrorGroup = mirrorGroups.getMirrorGroupArray(0);
    Assert.assertEquals(2, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(2, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(servers.getServerArray(0).getName(), mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals("server1", mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals(servers.getServerArray(1).getName(), mirrorGroup.getMembers().getMemberArray(1));
    Assert.assertEquals("server2", mirrorGroup.getMembers().getMemberArray(1));

    Assert.assertTrue(mirrorGroup.isSetHa());
    Ha ha = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.DISK_BASED_ACTIVE_PASSIVE, ha.getMode());

    mirrorGroup = mirrorGroups.getMirrorGroupArray(1);
    Assert.assertEquals(2, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(2, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(servers.getServerArray(2).getName(), mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals("server3", mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals(servers.getServerArray(3).getName(), mirrorGroup.getMembers().getMemberArray(1));
    Assert.assertEquals("server4", mirrorGroup.getMembers().getMemberArray(1));

    Assert.assertTrue(mirrorGroup.isSetHa());
    ha = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, ha.getMode());
    Assert.assertEquals(15, ha.getNetworkedActivePassive().getElectionTime());
  }

  public void testUpdateCheckDefault() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertTrue(servers.isSetUpdateCheck());
    Assert.assertEquals(true, servers.getUpdateCheck().getEnabled());
    Assert.assertEquals(7, servers.getUpdateCheck().getPeriodDays());
  }

  public void testUpdateCheck() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<servers>" 
                    +   "<server>"
                    +   "</server>" 
                    +   "<update-check>" 
                    +     "<enabled>false</enabled>" 
                    +     "<period-days>14</period-days>"
                    +   "</update-check>" 
                    + "</servers>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertTrue(servers.isSetUpdateCheck());
    Assert.assertEquals(false, servers.getUpdateCheck().getEnabled());
    Assert.assertEquals(14, servers.getUpdateCheck().getPeriodDays());
  }

  public void testClientNotInitialized() throws Exception {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertNull(client);

    configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);
    client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertNotNull(client);
  }

  public void testDefaultClientLogDirectory() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals(new File(ParameterSubstituter.substitute("logs-%i")).getAbsolutePath(), client.getLogs());
  }

  public void testClientLogDirectory() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<clients>"
                    +   "<logs>/abc/xyz/tra</logs>" 
                    + "</clients>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals("/abc/xyz/tra", client.getLogs());
  }
  
  public void testClientLogDirectory1() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<clients>"
                    +   "<logs>%i</logs>" 
                    + "</clients>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals(ParameterSubstituter.substitute("%i"), client.getLogs());
  }

  public void testTcNodeNameClientLogDirectory() throws Exception {
    this.tcConfig = getTempFile("default-config.xml");
    System.setProperty("tc.nodeName", "node123");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<clients>"
                    +   "<logs>%(tc.nodeName)</logs>" 
                    + "</clients>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals("node123", client.getLogs());
  }

  public void testDefaultFaultCount() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertTrue(client.isSetDso());
    Assert.assertEquals(500, client.getDso().getFaultCount());
  }

  public void testFaultCount() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<clients>" 
                    +   "<dso>"
                    +     "<fault-count>1234</fault-count>" 
                    +   "</dso>" 
                    + "</clients>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertTrue(client.isSetDso());
    Assert.assertEquals(1234, client.getDso().getFaultCount());
  }

  public void testDefaultClientDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertTrue(client.isSetDso());

    DsoClientData dso = client.getDso();
    Assert.assertTrue(dso.isSetFaultCount());
    Assert.assertTrue(dso.isSetDebugging());

    Assert.assertEquals(500, dso.getFaultCount());

    DsoClientDebugging debugging = dso.getDebugging();
    Assert.assertTrue(debugging.isSetInstrumentationLogging());
    Assert.assertTrue(debugging.isSetRuntimeLogging());
    Assert.assertTrue(debugging.isSetRuntimeOutputOptions());

    InstrumentationLogging instrumentationLogging = debugging.getInstrumentationLogging();
    Assert.assertTrue(instrumentationLogging.isSetClass1());
    Assert.assertTrue(instrumentationLogging.isSetDistributedMethods());
    Assert.assertTrue(instrumentationLogging.isSetHierarchy());
    Assert.assertTrue(instrumentationLogging.isSetLocks());
    Assert.assertTrue(instrumentationLogging.isSetRoots());
    Assert.assertTrue(instrumentationLogging.isSetTransientRoot());

    Assert.assertEquals(false, instrumentationLogging.getClass1());
    Assert.assertEquals(false, instrumentationLogging.getDistributedMethods());
    Assert.assertEquals(true, instrumentationLogging.getHierarchy());
    Assert.assertEquals(false, instrumentationLogging.getLocks());
    Assert.assertEquals(false, instrumentationLogging.getRoots());
    Assert.assertEquals(true, instrumentationLogging.getTransientRoot());

    RuntimeLogging runtimeLogging = debugging.getRuntimeLogging();
    Assert.assertTrue(runtimeLogging.isSetDistributedMethodDebug());
    Assert.assertTrue(runtimeLogging.isSetFieldChangeDebug());
    Assert.assertTrue(runtimeLogging.isSetLockDebug());
    Assert.assertTrue(runtimeLogging.isSetNamedLoaderDebug());
    Assert.assertTrue(runtimeLogging.isSetNewObjectDebug());
    Assert.assertTrue(runtimeLogging.isSetNonPortableDump());
    Assert.assertTrue(runtimeLogging.isSetWaitNotifyDebug());

    Assert.assertEquals(false, runtimeLogging.getDistributedMethodDebug());
    Assert.assertEquals(false, runtimeLogging.getFieldChangeDebug());
    Assert.assertEquals(false, runtimeLogging.getLockDebug());
    Assert.assertEquals(false, runtimeLogging.getNamedLoaderDebug());
    Assert.assertEquals(false, runtimeLogging.getNewObjectDebug());
    Assert.assertEquals(true, runtimeLogging.getNonPortableDump());
    Assert.assertEquals(false, runtimeLogging.getWaitNotifyDebug());

    RuntimeOutputOptions runtimeOutputOptions = debugging.getRuntimeOutputOptions();
    Assert.assertTrue(runtimeOutputOptions.isSetAutoLockDetails());
    Assert.assertTrue(runtimeOutputOptions.isSetCaller());
    Assert.assertTrue(runtimeOutputOptions.isSetFullStack());

    Assert.assertEquals(false, runtimeOutputOptions.getAutoLockDetails());
    Assert.assertEquals(false, runtimeOutputOptions.getCaller());
    Assert.assertEquals(false, runtimeOutputOptions.getFullStack());
  }

  public void testClientDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<clients>" 
                    +   "<dso>"
                    +     "<fault-count>1234</fault-count>" 
                    +     "<debugging>" 
                    +       "<instrumentation-logging>"
                    +         "<class>true</class>" 
                    +         "<locks>true</locks>" 
                    +         "<roots>true</roots>"
                    +       "</instrumentation-logging>" 
                    +       "<runtime-logging>"
                    +         "<non-portable-dump>false</non-portable-dump>" 
                    +         "<lock-debug>true</lock-debug>"
                    +         "<new-object-debug>true</new-object-debug>" 
                    +       "</runtime-logging>" 
                    +       "<runtime-output-options>"
                    +         "<caller>true</caller>" 
                    +         "<full-stack>true</full-stack>" 
                    +       "</runtime-output-options>"
                    +     "</debugging>" 
                    +   "</dso>" 
                    + "</clients>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertTrue(client.isSetDso());

    DsoClientData dso = client.getDso();
    Assert.assertTrue(dso.isSetFaultCount());
    Assert.assertTrue(dso.isSetDebugging());

    Assert.assertEquals(1234, dso.getFaultCount());

    DsoClientDebugging debugging = dso.getDebugging();
    Assert.assertTrue(debugging.isSetInstrumentationLogging());
    Assert.assertTrue(debugging.isSetRuntimeLogging());
    Assert.assertTrue(debugging.isSetRuntimeOutputOptions());

    InstrumentationLogging instrumentationLogging = debugging.getInstrumentationLogging();
    Assert.assertTrue(instrumentationLogging.isSetClass1());
    Assert.assertTrue(instrumentationLogging.isSetDistributedMethods());
    Assert.assertTrue(instrumentationLogging.isSetHierarchy());
    Assert.assertTrue(instrumentationLogging.isSetLocks());
    Assert.assertTrue(instrumentationLogging.isSetRoots());
    Assert.assertTrue(instrumentationLogging.isSetTransientRoot());

    Assert.assertEquals(true, instrumentationLogging.getClass1());
    Assert.assertEquals(false, instrumentationLogging.getDistributedMethods());
    Assert.assertEquals(true, instrumentationLogging.getHierarchy());
    Assert.assertEquals(true, instrumentationLogging.getLocks());
    Assert.assertEquals(true, instrumentationLogging.getRoots());
    Assert.assertEquals(true, instrumentationLogging.getTransientRoot());

    RuntimeLogging runtimeLogging = debugging.getRuntimeLogging();
    Assert.assertTrue(runtimeLogging.isSetDistributedMethodDebug());
    Assert.assertTrue(runtimeLogging.isSetFieldChangeDebug());
    Assert.assertTrue(runtimeLogging.isSetLockDebug());
    Assert.assertTrue(runtimeLogging.isSetNamedLoaderDebug());
    Assert.assertTrue(runtimeLogging.isSetNewObjectDebug());
    Assert.assertTrue(runtimeLogging.isSetNonPortableDump());
    Assert.assertTrue(runtimeLogging.isSetWaitNotifyDebug());

    Assert.assertEquals(false, runtimeLogging.getDistributedMethodDebug());
    Assert.assertEquals(false, runtimeLogging.getFieldChangeDebug());
    Assert.assertEquals(true, runtimeLogging.getLockDebug());
    Assert.assertEquals(false, runtimeLogging.getNamedLoaderDebug());
    Assert.assertEquals(true, runtimeLogging.getNewObjectDebug());
    Assert.assertEquals(false, runtimeLogging.getNonPortableDump());
    Assert.assertEquals(false, runtimeLogging.getWaitNotifyDebug());

    RuntimeOutputOptions runtimeOutputOptions = debugging.getRuntimeOutputOptions();
    Assert.assertTrue(runtimeOutputOptions.isSetAutoLockDetails());
    Assert.assertTrue(runtimeOutputOptions.isSetCaller());
    Assert.assertTrue(runtimeOutputOptions.isSetFullStack());

    Assert.assertEquals(false, runtimeOutputOptions.getAutoLockDetails());
    Assert.assertEquals(true, runtimeOutputOptions.getCaller());
    Assert.assertEquals(true, runtimeOutputOptions.getFullStack());
  }

  public void testClientDsoOverridden() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<clients>" 
                    +   "<dso>"
                    +     "<fault-count>1234</fault-count>" 
                    +     "<debugging>" 
                    +       "<instrumentation-logging>"
                    +         "<class>true</class>" 
                    +         "<hierarchy>false</hierarchy>"
                    +         "<distributed-methods>true</distributed-methods>" 
                    +         "<transient-root>false</transient-root>"
                    +         "<locks>true</locks>" 
                    +         "<roots>true</roots>" 
                    +       "</instrumentation-logging>"
                    +       "<runtime-logging>" 
                    +         "<non-portable-dump>false</non-portable-dump>"
                    +         "<lock-debug>true</lock-debug>" 
                    +         "<field-change-debug>true</field-change-debug>"
                    +         "<wait-notify-debug>true</wait-notify-debug>"
                    +         "<distributed-method-debug>true</distributed-method-debug>"
                    +         "<new-object-debug>true</new-object-debug>" 
                    +         "<named-loader-debug>true</named-loader-debug>"
                    +       "</runtime-logging>" 
                    +       "<runtime-output-options>" 
                    +         "<auto-lock-details>true</auto-lock-details>"
                    +         "<caller>true</caller>" 
                    +         "<full-stack>true</full-stack>" 
                    +       "</runtime-output-options>"
                    +     "</debugging>" 
                    +   "</dso>" 
                    + "</clients>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertTrue(client.isSetDso());

    DsoClientData dso = client.getDso();
    Assert.assertTrue(dso.isSetFaultCount());
    Assert.assertTrue(dso.isSetDebugging());

    Assert.assertEquals(1234, dso.getFaultCount());

    DsoClientDebugging debugging = dso.getDebugging();
    Assert.assertTrue(debugging.isSetInstrumentationLogging());
    Assert.assertTrue(debugging.isSetRuntimeLogging());
    Assert.assertTrue(debugging.isSetRuntimeOutputOptions());

    InstrumentationLogging instrumentationLogging = debugging.getInstrumentationLogging();
    Assert.assertTrue(instrumentationLogging.isSetClass1());
    Assert.assertTrue(instrumentationLogging.isSetDistributedMethods());
    Assert.assertTrue(instrumentationLogging.isSetHierarchy());
    Assert.assertTrue(instrumentationLogging.isSetLocks());
    Assert.assertTrue(instrumentationLogging.isSetRoots());
    Assert.assertTrue(instrumentationLogging.isSetTransientRoot());

    Assert.assertEquals(true, instrumentationLogging.getClass1());
    Assert.assertEquals(true, instrumentationLogging.getDistributedMethods());
    Assert.assertEquals(false, instrumentationLogging.getHierarchy());
    Assert.assertEquals(true, instrumentationLogging.getLocks());
    Assert.assertEquals(true, instrumentationLogging.getRoots());
    Assert.assertEquals(false, instrumentationLogging.getTransientRoot());

    RuntimeLogging runtimeLogging = debugging.getRuntimeLogging();
    Assert.assertTrue(runtimeLogging.isSetDistributedMethodDebug());
    Assert.assertTrue(runtimeLogging.isSetFieldChangeDebug());
    Assert.assertTrue(runtimeLogging.isSetLockDebug());
    Assert.assertTrue(runtimeLogging.isSetNamedLoaderDebug());
    Assert.assertTrue(runtimeLogging.isSetNewObjectDebug());
    Assert.assertTrue(runtimeLogging.isSetNonPortableDump());
    Assert.assertTrue(runtimeLogging.isSetWaitNotifyDebug());

    Assert.assertEquals(true, runtimeLogging.getDistributedMethodDebug());
    Assert.assertEquals(true, runtimeLogging.getFieldChangeDebug());
    Assert.assertEquals(true, runtimeLogging.getLockDebug());
    Assert.assertEquals(true, runtimeLogging.getNamedLoaderDebug());
    Assert.assertEquals(true, runtimeLogging.getNewObjectDebug());
    Assert.assertEquals(false, runtimeLogging.getNonPortableDump());
    Assert.assertEquals(true, runtimeLogging.getWaitNotifyDebug());

    RuntimeOutputOptions runtimeOutputOptions = debugging.getRuntimeOutputOptions();
    Assert.assertTrue(runtimeOutputOptions.isSetAutoLockDetails());
    Assert.assertTrue(runtimeOutputOptions.isSetCaller());
    Assert.assertTrue(runtimeOutputOptions.isSetFullStack());

    Assert.assertEquals(true, runtimeOutputOptions.getAutoLockDetails());
    Assert.assertEquals(true, runtimeOutputOptions.getCaller());
    Assert.assertEquals(true, runtimeOutputOptions.getFullStack());
  }

  public void testSystemDefaultConfigModel() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    com.terracottatech.config.System system = (com.terracottatech.config.System) configSetupMgr.systemBeanRepository()
        .bean();

    Assert.assertTrue(system.isSetConfigurationModel());
    Assert.assertEquals(ConfigurationModel.DEVELOPMENT, system.getConfigurationModel());
  }

  public void testSystemConfigModel() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<system>"
                    +   "<configuration-model>production</configuration-model>" 
                    + "</system>" 
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    com.terracottatech.config.System system = (com.terracottatech.config.System) configSetupMgr.systemBeanRepository()
        .bean();

    Assert.assertTrue(system.isSetConfigurationModel());
    Assert.assertEquals(ConfigurationModel.PRODUCTION, system.getConfigurationModel());
  }

  public void testApplicationDefaults() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);
    for (Iterator i = configSetupMgr.applicationsRepository().applicationNames(); i.hasNext();) {
      Application application = (Application) configSetupMgr.applicationsRepository().repositoryFor((String) i.next())
          .bean();
      Assert.assertTrue(application.isSetDso());
      Assert.assertEquals(true, application.getDso().getDsoReflectionEnabled());
    }
  }

  public void testApplication() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                    + "<application>" 
                    + "<dso>"
                    +   "<dso-reflection-enabled>false</dso-reflection-enabled>" 
                    + "</dso>" 
                    + "</application>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);
    for (Iterator i = configSetupMgr.applicationsRepository().applicationNames(); i.hasNext();) {
      Application application = (Application) configSetupMgr.applicationsRepository().repositoryFor((String) i.next())
          .bean();
      Assert.assertTrue(application.isSetDso());
      Assert.assertEquals(false, application.getDso().getDsoReflectionEnabled());
    }
  }

  private BaseConfigurationSetupManager initializeAndGetBaseTVSConfigSetupManager(boolean isClient)
      throws ConfigurationSetupException {
    String[] args = new String[] { "-f", tcConfig.getAbsolutePath() };

    String effectiveConfigSpec = getEffectiveConfigSpec(
                                                        System
                                                            .getProperty(ConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME),
                                                        parseDefaultCommandLine(
                                                                                args,
                                                                                StandardConfigurationSetupManagerFactory.ConfigMode.L2),
                                                        StandardConfigurationSetupManagerFactory.ConfigMode.L2);
    String cwdAsString = System.getProperty("user.dir");
    if (StringUtils.isBlank(cwdAsString)) { throw new ConfigurationSetupException(
                                                                                  "We can't find the working directory of the process; we need this to continue. "
                                                                                      + "(The system property 'user.dir' was "
                                                                                      + (cwdAsString == null ? "null"
                                                                                          : "'" + cwdAsString + "'")
                                                                                      + ".)"); }
    ConfigurationSpec configurationSpec = new ConfigurationSpec(effectiveConfigSpec, System
        .getProperty(ConfigurationSetupManagerFactory.SERVER_CONFIG_FILE_PROPERTY_NAME),
                                                                StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new File(cwdAsString));

    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(
                                                                                        configurationSpec,
                                                                                        new TerracottaDomainConfigurationDocumentBeanFactory());

    BaseConfigurationSetupManager configSetupMgr = new BaseConfigurationSetupManager(
                                                                                     configurationCreator,
                                                                                     new SchemaDefaultValueProvider(),
                                                                                     new StandardXmlObjectComparator(),
                                                                                     new FatalIllegalConfigurationChangeHandler());
    configSetupMgr.runConfigurationCreator(isClient);

    return configSetupMgr;
  }

  @Override
  protected File getTempFile(String fileName) throws IOException {
    return getTempDirectoryHelper().getFile(fileName);
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

  private String getEffectiveConfigSpec(final String configSpec, final CommandLine commandLine,
                                        final ConfigMode configMode) throws ConfigurationSetupException {

    String configFileOnCommandLine = null;
    String effectiveConfigSpec;

    configFileOnCommandLine = StringUtils.trimToNull(commandLine.getOptionValue('f'));
    effectiveConfigSpec = StringUtils
        .trimToNull(configFileOnCommandLine != null ? configFileOnCommandLine : configSpec);

    if (StringUtils.isBlank(effectiveConfigSpec)) {
      File localConfig = new File(System.getProperty("user.dir"), DEFAULT_CONFIG_SPEC);

      if (localConfig.exists()) {
        effectiveConfigSpec = localConfig.getAbsolutePath();
      } else if (configMode == ConfigMode.L2) {
        effectiveConfigSpec = DEFAULT_CONFIG_URI;
      }
    }

    if (StringUtils.isBlank(effectiveConfigSpec)) {
      // formatting
      throw new ConfigurationSetupException("You must specify the location of the Terracotta "
                                            + "configuration file for this process, using the " + "'"
                                            + CONFIG_FILE_PROPERTY_NAME + "' system property.");
    }

    return effectiveConfigSpec;
  }

  private static CommandLine parseDefaultCommandLine(String[] args, ConfigMode configMode)
      throws ConfigurationSetupException {
    try {
      if (args == null || args.length == 0) {
        return new PosixParser().parse(new Options(), new String[0]);
      } else {
        Options options = createOptions(configMode);

        return new PosixParser().parse(options, args);
      }
    } catch (ParseException pe) {
      throw new ConfigurationSetupException(pe.getLocalizedMessage(), pe);
    }
  }

  private static Options createOptions(ConfigMode configMode) {
    Options options = new Options();

    Option configFileOption = new Option("f", CONFIG_SPEC_ARGUMENT_NAME, true,
                                         "the configuration file to use, specified as a file path or URL");
    configFileOption.setArgName("file-or-URL");
    configFileOption.setType(String.class);

    if (configMode == ConfigMode.L2) {
      configFileOption.setRequired(false);
      options.addOption(configFileOption);

      Option l2NameOption = new Option("n", "name", true, "the name of this L2; defaults to the host name");
      l2NameOption.setRequired(false);
      l2NameOption.setArgName("l2-name");
      options.addOption(l2NameOption);
    } else {
      configFileOption.setRequired(true);
      options.addOption(configFileOption);
    }

    return options;
  }
}
