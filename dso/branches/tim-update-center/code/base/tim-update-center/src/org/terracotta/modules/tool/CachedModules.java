/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link Modules} that uses a cached XML file as its data source.
 */
class CachedModules implements Modules {

  private final Map<ModuleId, Module> modules;

  private final String                tcVersion;

  public CachedModules(InputStream data, String tcVersion) throws JDOMException, IOException {
    this.tcVersion = tcVersion;

    SAXBuilder builder = new SAXBuilder();
    Document document = builder.build(data);
    Element root = document.getRootElement();
    this.modules = new HashMap<ModuleId, Module>();

    List<Element> children = root.getChildren();
    for (Element child : children) {
      Module module = Module.create(this, child);
      if (!qualify(module)) continue;
      this.modules.put(module.getId(), module);
    }
  }

  private boolean qualify(Module module) {
    return module.getTcVersion().equals("*") || module.getTcVersion().equals(this.tcVersion);
  }

  public String tcVersion() {
    return tcVersion;
  }

  public Module get(ModuleId id) {
    return this.modules.get(id);
  }

  public List<Module> list() {
    List<Module> list = new ArrayList<Module>(this.modules.values());
    Collections.sort(list);
    return list;
  }

  public List<Module> listLatest() {
    List<Module> list = list();
    Map<String, Module> group = new HashMap<String, Module>();
    for (Module module : list) {
      Module other = group.get(module.getSymbolicName());
      if (other == null) {
        group.put(module.getSymbolicName(), module);
        continue;
      }
      if (module.isOlder(other)) continue;
      group.put(module.getSymbolicName(), module);
    }
    list = new ArrayList<Module>(group.values());
    Collections.sort(list);
    return list;
  }

  /**
   * Return a list of modules matching the groupId and artifactId.
   * 
   * @param groupId
   * @param artifactId
   */
  public List<Module> get(String groupId, String artifactId) {
    List<Module> list = new ArrayList<Module>();
    for (Module module : list()) {
      if (!module.getSymbolicName().equals(ModuleId.computeSymbolicName(groupId, artifactId))) continue;
      list.add(module);
    }
    Collections.sort(list);
    return list;
  }

}
