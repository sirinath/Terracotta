/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.google.inject.Inject;
//import com.tc.util.ProductInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link Modules} that uses a cached XML file as its data source.
 */
public class CachedXmlModules implements Modules {

  private final Map<ModuleId, Module> modules;

  @Inject
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

  public Set<ModuleId> getModuleFamilies() {
    Set<ModuleId> families = new HashSet<ModuleId>();
    for (ModuleId id : this.modules.keySet()) {
      ModuleId familyId = new ModuleId(id.getGroupId(), id.getArtifactId(), null);
      families.add(familyId);
    }
    return families;
  }

  public List<Module> list() {
    return new ArrayList<Module>(this.modules.values());
  }

  public Module getModuleById(ModuleId id) {
    return this.modules.get(id);
  }

  public List<ModuleId> getModuleIdsInFamily(ModuleId familyId) {
    List<ModuleId> result = new ArrayList<ModuleId>();
    for (ModuleId moduleId : this.modules.keySet()) {
      if (moduleId.isSibling(familyId)) {
        result.add(familyId);
      }
    }
    return result;
  }

  public List<Module> listLatest() {
    Set<ModuleId> families = this.getModuleFamilies();
    List<Module> latest = new ArrayList();
    for (ModuleId id : families) {
      latest.add(this.getLatestInFamily(id));
    }
    return latest;
  }

  public Module getLatestInFamily(ModuleId family) {
    List<ModuleId> siblings = this.getModuleIdsInFamily(family);
    Collections.sort(siblings);
    return this.getModuleById(siblings.get(siblings.size() - 1));
  }

  private String tcVersion() {
    //return ProductInfo.getInstance().version();
    return "2.6.2";
  }
}
