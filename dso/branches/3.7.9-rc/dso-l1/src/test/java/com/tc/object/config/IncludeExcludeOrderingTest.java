/*
 * All content copyright (c) 2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package com.tc.object.config;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.values.XmlValueOutOfRangeException;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.config.Loader;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.object.BaseDSOTestCase;
import com.terracottatech.config.Application;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.IOException;
import java.io.InputStream;

public class IncludeExcludeOrderingTest extends BaseDSOTestCase {

  /**
   * Verify that relative order of includes and excludes is preserved: a specific include placed after a more general
   * exclude will win, and similarly a specific exclude will win after a general include.
   */
  public void testMoreSpecificOrdering() throws XmlException, IOException, ConfigurationSetupException {
    DSOClientConfigHelper config = loadConfigFile("tc-config-includeexclude.xml");

    System.out.println("The following warnings about unloadable classes [p/A*] are expected.");
    ClassInfo classInfoA = AsmClassInfo.getClassInfo("p.A", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoA));
    
    ClassInfo classInfoB = AsmClassInfo.getClassInfo("p.q.B", getClass().getClassLoader());
    assertFalse(config.shouldBeAdapted(classInfoB));
    
    ClassInfo classInfoC = AsmClassInfo.getClassInfo("p.q.C", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoC));
    
    ClassInfo classInfoD = AsmClassInfo.getClassInfo("p.q.r.D", getClass().getClassLoader());
    assertFalse(config.shouldBeAdapted(classInfoD));
    
    ClassInfo classInfoE = AsmClassInfo.getClassInfo("p.q.r.E", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoE));
  }

  /**
   * Verify that relative order of includes and excludes is preserved: a more general include placed after a more
   * specific exclude will win, and similarly a more general exclude after a specific include. Note that this sort of
   * content in a tc-config.xml is not very useful, and indeed at some point we might want to issue warnings.
   */
  public void testMoreGeneralOrdering() throws XmlException, IOException, ConfigurationSetupException {
    DSOClientConfigHelper config = loadConfigFile("tc-config-includeexclude2.xml");

    System.out.println("The following warnings about unloadable classes [A*] and [Z*] are expected.");
    ClassInfo classInfoA = AsmClassInfo.getClassInfo("A", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoA));
    
    ClassInfo classInfoC = AsmClassInfo.getClassInfo("ABC", getClass().getClassLoader());
    assertTrue(config.shouldBeAdapted(classInfoC));
    
    ClassInfo classInfoZ = AsmClassInfo.getClassInfo("Z", getClass().getClassLoader());
    assertFalse(config.shouldBeAdapted(classInfoZ));

    ClassInfo classInfoY = AsmClassInfo.getClassInfo("ZY", getClass().getClassLoader());
    assertFalse(config.shouldBeAdapted(classInfoY));
  }

  /**
   * Load a config file in the same way that normal instrumention is done.
   * @see com.tc.ModulesLoader#loadConfiguration()
   */
  private DSOClientConfigHelper loadConfigFile(String fileName) throws IOException, XmlException,
      ConfigurationSetupException {
    InputStream configFile = getClass().getResourceAsStream(fileName);
    TcConfigDocument tcConfigDocument = new Loader().parse(configFile, new XmlOptions().setLoadLineNumbers()
        .setValidateOnSet());
    TcConfig tcConfig = tcConfigDocument.getTcConfig();
    Application application = tcConfig.getApplication();
    assertNotNull("<application> tag not found - check file " + fileName, application);
    TCLogger logger = new NullTCLogger();
    DSOClientConfigHelper config = createClientConfigHelper();

    ConfigLoader loader = new ConfigLoader(config, logger);
    try {
      loader.loadDsoConfig(application.getDso());
    } catch (XmlValueOutOfRangeException e) {
      fail(e.getMessage());
    }
    return config;
  }

}
