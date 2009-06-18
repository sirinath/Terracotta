package com.tc.modules;

import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.Locks;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;
import com.terracottatech.config.NamedLock;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.WebApplication;
import com.terracottatech.config.WebApplications;
import com.terracottatech.config.DistributedMethods.MethodExpression;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class ModulesConfiguration extends ModuleInfoGroup {
  private Modules                                   modules;
  private final LinkedHashMap<File, RepositoryInfo> repositoryInfoMap = new LinkedHashMap();
  private final DsoApplication                      application       = com.terracottatech.config.DsoApplication.Factory
                                                                          .newInstance();

  public void setModules(Modules modules) {
    this.modules = modules;
    for (String repo : modules.getRepositoryArray()) {
      addRepository(repo);
    }
  }

  public Modules getModules() {
    return modules;
  }

  public Module addNewModule(String artifactId, String groupId, String version) {
    Module module = modules.addNewModule();
    module.setName(artifactId);
    module.setGroupId(groupId);
    module.setVersion(version);
    add(module);
    return module;
  }

  public boolean addRepository(String repoDir) {
    return addRepository(new File(ParameterSubstituter.substitute(repoDir)));
  }

  public boolean addRepository(File repoDir) {
    if (!repositoryInfoMap.containsKey(repoDir)) {
      repositoryInfoMap.put(repoDir, new RepositoryInfo(repoDir));
      return true;
    } else {
      return false;
    }
  }

  public RepositoryInfo getRepository(String repoDirPath) {
    return repositoryInfoMap.get(new File(repoDirPath));
  }

  public RepositoryInfo getRepository(File repoDir) {
    return repositoryInfoMap.get(repoDir);
  }

  public RepositoryInfo removeRepository(File repoDir) {
    return repositoryInfoMap.remove(repoDir);
  }

  public Iterator repositoryInfoIterator() {
    return repositoryInfoMap.values().iterator();
  }

  @Override
  public void setModuleApplication(ModuleInfo moduleInfo, DsoApplication application) {
    super.setModuleApplication(moduleInfo, application);
    merge(application);
  }

  public DsoApplication getApplication() {
    return application;
  }

  public void merge(DsoApplication toMerge) {
    merge(toMerge, application);
  }

  public static void merge(DsoApplication src, DsoApplication dest) {
    if (src.isSetInstrumentedClasses()) {
      InstrumentedClasses srcInstrumentedClasses = src.getInstrumentedClasses();
      InstrumentedClasses destInstrumentedClasses = dest.isSetInstrumentedClasses() ? dest.getInstrumentedClasses()
          : dest.addNewInstrumentedClasses();
      merge(srcInstrumentedClasses, destInstrumentedClasses);
    }
    if (src.isSetTransientFields()) {
      TransientFields srcTransientFields = src.getTransientFields();
      TransientFields destTransientFields = dest.isSetTransientFields() ? dest.getTransientFields() : dest
          .addNewTransientFields();
      merge(srcTransientFields, destTransientFields);
    }
    if (src.isSetLocks()) {
      Locks srcLocks = src.getLocks();
      Locks destLocks = dest.isSetLocks() ? dest.getLocks() : dest.addNewLocks();
      merge(srcLocks, destLocks);
    }
    if (src.isSetRoots()) {
      Roots srcRoots = src.getRoots();
      Roots destRoots = dest.isSetRoots() ? dest.getRoots() : dest.addNewRoots();
      merge(srcRoots, destRoots);
    }
    if (src.isSetDistributedMethods()) {
      DistributedMethods srcDistributedMethods = src.getDistributedMethods();
      DistributedMethods destDistributedMethods = dest.isSetDistributedMethods() ? dest.getDistributedMethods() : dest
          .addNewDistributedMethods();
      merge(srcDistributedMethods, destDistributedMethods);
    }
    if (src.isSetAdditionalBootJarClasses()) {
      AdditionalBootJarClasses srcBootClasses = src.getAdditionalBootJarClasses();
      AdditionalBootJarClasses destBootClasses = dest.isSetAdditionalBootJarClasses() ? dest
          .getAdditionalBootJarClasses() : dest.addNewAdditionalBootJarClasses();
      merge(srcBootClasses, destBootClasses);
    }
    if (src.isSetWebApplications()) {
      WebApplications srcWebApps = src.getWebApplications();
      WebApplications destWebApps = dest.isSetWebApplications() ? dest.getWebApplications() : dest
          .addNewWebApplications();
      merge(srcWebApps, destWebApps);
    }
    if (src.isSetDsoReflectionEnabled()) dest.setDsoReflectionEnabled(src.getDsoReflectionEnabled());
  }

  public static void merge(InstrumentedClasses src, InstrumentedClasses dest) {
    List includeList = new ArrayList(Arrays.asList(src.getIncludeArray()));
    for (Include include : dest.getIncludeArray()) {
      includeList.add(include.copy());
    }
    dest.setIncludeArray((Include[]) includeList.toArray(new Include[0]));
  }

  public static void merge(TransientFields src, TransientFields dest) {
    List fieldList = new ArrayList(Arrays.asList(src.getFieldNameArray()));
    fieldList.addAll(Arrays.asList(dest.getFieldNameArray()));
    dest.setFieldNameArray((String[]) fieldList.toArray(new String[0]));
  }

  public static void merge(Locks src, Locks dest) {
    List autolockList = new ArrayList(Arrays.asList(src.getAutolockArray()));
    for (Autolock autolock : dest.getAutolockArray()) {
      autolockList.add(autolock.copy());
    }
    dest.setAutolockArray((Autolock[]) autolockList.toArray(new Autolock[0]));

    List namedLockList = new ArrayList(Arrays.asList(src.getNamedLockArray()));
    for (NamedLock namedLock : dest.getNamedLockArray()) {
      namedLockList.add(namedLock.copy());
    }
    dest.setNamedLockArray((NamedLock[]) namedLockList.toArray(new NamedLock[0]));
  }

  public static void merge(Roots src, Roots dest) {
    List rootList = new ArrayList(Arrays.asList(src.getRootArray()));
    for (Root root : dest.getRootArray()) {
      rootList.add(root.copy());
    }
    dest.setRootArray((Root[]) rootList.toArray(new Root[0]));
  }

  public static void merge(DistributedMethods src, DistributedMethods dest) {
    List exprList = new ArrayList(Arrays.asList(src.getMethodExpressionArray()));
    for (MethodExpression expr : dest.getMethodExpressionArray()) {
      exprList.add(expr.copy());
    }
    dest.setMethodExpressionArray((MethodExpression[]) exprList.toArray(new MethodExpression[0]));
  }

  public static void merge(AdditionalBootJarClasses src, AdditionalBootJarClasses dest) {
    List includeList = new ArrayList(Arrays.asList(src.getIncludeArray()));
    includeList.addAll(Arrays.asList(dest.getIncludeArray()));
    dest.setIncludeArray((String[]) includeList.toArray(new String[0]));
  }

  public static void merge(WebApplications src, WebApplications dest) {
    List webAppList = new ArrayList(Arrays.asList(src.getWebApplicationArray()));
    for (WebApplication webApp : dest.getWebApplicationArray()) {
      webAppList.add(webApp.copy());
    }
    dest.setWebApplicationArray((WebApplication[]) webAppList.toArray(new WebApplication[0]));
  }
}
