/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class ModuleTest extends TestCase {

  public void testGetSiblings() {
    Modules modules = getModules("2.5.4", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(4, modules.list().size());

    ModuleId id = ModuleId.create("org.terracotta.modules", "tim-annotations", "1.0.3");
    Module module = modules.get(id);
    assertNotNull(module);
    List<Module> siblings = module.getSiblings();
    assertNotNull(siblings);
    assertTrue(siblings.isEmpty());

    id = ModuleId.create("org.terracotta.modules", "tim-apache-struts-1.1", "1.0.1");
    module = modules.get(id);
    assertNotNull(module);
    siblings = module.getSiblings();
    assertNotNull(siblings);
    assertFalse(siblings.isEmpty());
    assertEquals(2, siblings.size());
    for (Module sibling : siblings) {
      String symname = ModuleId.computeSymbolicName("org.terracotta.modules", "tim-apache-struts-1.1");
      assertTrue(sibling.getSymbolicName().equals(symname));
      assertTrue(sibling.isSibling(module));
    }
  }

  public void testGetVersions() {
    Modules modules = getModules("2.5.0", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());

    ModuleId id = ModuleId.create("org.terracotta.modules", "tim-annotations", "1.0.0");
    Module module = modules.get(id);
    assertNotNull(module);
    List<String> versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(1, versions.size());
    assertTrue(versions.get(0).equals("1.0.1"));

    id = ModuleId.create("org.terracotta.modules", "tim-annotations", "1.0.1");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(1, versions.size());
    assertTrue(versions.get(0).equals("1.0.0"));

    modules = getModules("2.5.4", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(4, modules.list().size());

    id = ModuleId.create("org.terracotta.modules", "tim-annotations", "1.0.3");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertTrue(versions.isEmpty());

    id = ModuleId.create("org.terracotta.modules", "tim-apache-struts-1.1", "1.0.1");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(2, versions.size());
    assertTrue(versions.get(0).equals("1.0.2"));
    assertTrue(versions.get(1).equals("1.0.3"));

    id = ModuleId.create("org.terracotta.modules", "tim-apache-struts-1.1", "1.0.2");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(2, versions.size());
    assertTrue(versions.get(0).equals("1.0.1"));
    assertTrue(versions.get(1).equals("1.0.3"));

    id = ModuleId.create("org.terracotta.modules", "tim-apache-struts-1.1", "1.0.3");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(2, versions.size());
    assertTrue(versions.get(0).equals("1.0.1"));
    assertTrue(versions.get(1).equals("1.0.2"));
  }

  private Modules getModules(String tcversion, String file) {
    try {
      InputStream data = this.getClass().getResourceAsStream(file);
      assertNotNull(data);
      return new CachedModules(data, tcversion);
    } catch (Exception e) {
      return null;
    }
  }
}
