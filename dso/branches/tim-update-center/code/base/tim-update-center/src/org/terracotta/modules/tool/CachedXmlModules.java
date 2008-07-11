/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.tc.util.ProductInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link Modules} that uses a cached XML file as its data source.
 */
public class CachedXmlModules implements Modules {

  private final Map<ModuleId, Module> modules;

  public List<Module> list() {
    return new ArrayList<Module>(this.modules.values());
  }

  public Module getModuleById(ModuleId id) {
    return this.modules.get(id);
  }

  public List<Module> getSiblingModules(ModuleId id) {
    List<Module> result = new ArrayList<Module>();
    for (ModuleId moduleId : this.modules.keySet()) {
      if (moduleId.isSibling(id)) {
        result.add(this.modules.get(id));
      }
    }
    return result;
  }

  public List<Module> listLatest() {
    List<Module> list = this.list();
    return null;
  }

  public CachedXmlModules(File data) throws JDOMException, IOException {
    SAXBuilder builder = new SAXBuilder();
    Document document = builder.build(data);
    Element root = document.getRootElement();
    this.modules = new HashMap<ModuleId, Module>();

    List<Element> children = root.getChildren();
    for (Element child : children) {
      Module module = Module.create(child);
      if (module.isCompatible(tcVersion())) {
        this.modules.put(module.getId(), module);
      }
    }
  }

  private String tcVersion() {
    return ProductInfo.getInstance().version();
  }
}
