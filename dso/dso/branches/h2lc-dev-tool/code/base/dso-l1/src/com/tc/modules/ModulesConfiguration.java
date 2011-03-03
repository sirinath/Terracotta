/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
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
  private final LinkedHashMap<File, RepositoryInfo> repositoryInfoMap;
  private final DsoApplication                      application;

  public ModulesConfiguration() {
    super();
    repositoryInfoMap = new LinkedHashMap<File, RepositoryInfo>();
    application = DsoApplication.Factory.newInstance();
  }

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
    }
    return false;
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

  public Iterator<RepositoryInfo> repositoryInfoIterator() {
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

    if (src.isSetDsoReflectionEnabled()) {
      dest.setDsoReflectionEnabled(src.getDsoReflectionEnabled());
    }
  }

  public static void merge(InstrumentedClasses src, InstrumentedClasses dest) {
    Include[] includes = src.getIncludeArray();
    List<Include> includeList = new ArrayList(Arrays.asList(includes));

    for (Include include : dest.getIncludeArray()) {
      includeList.add((Include) include.copy());
    }
    dest.setIncludeArray(includeList.toArray(new Include[0]));
  }

  public static void merge(TransientFields src, TransientFields dest) {
    String[] fields = src.getFieldNameArray();
    List<String> fieldList = new ArrayList(Arrays.asList(fields));

    fieldList.addAll(Arrays.asList(dest.getFieldNameArray()));
    dest.setFieldNameArray(fieldList.toArray(new String[0]));
  }

  public static void merge(Locks src, Locks dest) {
    Autolock[] autolocks = src.getAutolockArray();
    List<Autolock> autolockList = new ArrayList(Arrays.asList(autolocks));

    for (Autolock autolock : dest.getAutolockArray()) {
      autolockList.add((Autolock) autolock.copy());
    }
    dest.setAutolockArray(autolockList.toArray(new Autolock[0]));

    NamedLock[] namedLocks = src.getNamedLockArray();
    List<NamedLock> namedLockList = new ArrayList(Arrays.asList(namedLocks));

    for (NamedLock namedLock : dest.getNamedLockArray()) {
      namedLockList.add((NamedLock) namedLock.copy());
    }
    dest.setNamedLockArray(namedLockList.toArray(new NamedLock[0]));
  }

  public static void merge(Roots src, Roots dest) {
    Root[] roots = src.getRootArray();
    List<Root> rootList = new ArrayList(Arrays.asList(roots));

    for (Root root : dest.getRootArray()) {
      rootList.add((Root) root.copy());
    }
    dest.setRootArray(rootList.toArray(new Root[0]));
  }

  public static void merge(DistributedMethods src, DistributedMethods dest) {
    MethodExpression[] methodExprs = src.getMethodExpressionArray();
    List<MethodExpression> exprList = new ArrayList(Arrays.asList(methodExprs));

    for (MethodExpression expr : dest.getMethodExpressionArray()) {
      exprList.add((MethodExpression) expr.copy());
    }
    dest.setMethodExpressionArray(exprList.toArray(new MethodExpression[0]));
  }

  public static void merge(AdditionalBootJarClasses src, AdditionalBootJarClasses dest) {
    String[] includes = src.getIncludeArray();
    List<String> includeList = new ArrayList(Arrays.asList(includes));

    includeList.addAll(Arrays.asList(dest.getIncludeArray()));
    dest.setIncludeArray(includeList.toArray(new String[0]));
  }

  public static void merge(WebApplications src, WebApplications dest) {
    WebApplication[] webApps = src.getWebApplicationArray();
    List<WebApplication> webAppList = new ArrayList(Arrays.asList(webApps));

    for (WebApplication webApp : dest.getWebApplicationArray()) {
      webAppList.add((WebApplication) webApp.copy());
    }
    dest.setWebApplicationArray(webAppList.toArray(new WebApplication[0]));
  }

}
