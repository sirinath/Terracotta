/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;
/*
import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.config.schema.NewCommonL1Config;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.LiteralValues;
import com.tc.object.SerializationUtil;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.ExcludedInstrumentedClass;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.config.schema.IncludedInstrumentedClass;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.config.schema.NewDSOApplicationConfig;
import com.tc.object.config.schema.NewSpringApplicationConfig;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ClassUtils.ClassSpec;
import com.tc.util.runtime.Vm;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;
import com.terracottatech.config.SpringApplication;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
*/

public interface IStandardDSOClientConfigHelper {
  /**
  public void allowCGLIBInstrumentation();

  public Portability getPortability();

  // This is used only for tests right now
  public void addIncludePattern(String expression);
  
  // This is used only for tests right now
  public NewCommonL1Config getNewCommonL1Config();

  // This is used only for tests right now
  public void addIncludePattern(String expression, boolean honorTransient);
  
  // This is used only for tests right now
  public void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad,
                                boolean honorVolatile);
  
  public void addIncludeAndLockIfRequired(String expression, boolean honorTransient,
                                          boolean oldStyleCallConstructorOnLoad, boolean honorVolatile,
                                          String lockExpression, ClassInfo classInfo);

  // This is used only for tests right now
  public void addExcludePattern(String expression);
  
  // This is used only for tests right now
  public void addInstrumentationDescriptor(InstrumentedClass classDesc);
  
  // This is used only for tests right now
  public boolean hasIncludeExcludePatterns();

  // This is used only for tests right now
  public boolean hasIncludeExcludePattern(ClassInfo classInfo);
  
  public DSORuntimeLoggingOptions runtimeLoggingOptions();

  public DSORuntimeOutputOptions runtimeOutputOptions();

  public DSOInstrumentationLoggingOptions instrumentationLoggingOptions();

  public boolean removeCustomAdapter(String name);

  public void addCustomAdapter(String name, ClassAdapterFactory factory);
  
  public DSOInstrumentationLoggingOptions getInstrumentationLoggingOptions();
  
  public Iterator getAllUserDefinedBootSpecs();
  
  public void setFaultCount(int count);

  public boolean isLockMethod(MemberInfo memberInfo);
  
  public boolean matches(final Lock lock, final MemberInfo methodInfo);

  public boolean matches(final String expression, final MemberInfo methodInfo);

   // This is a simplified interface from DSOApplicationConfig. This is used for programmatically generating config.
  public void addRoot(String rootName, String rootFieldName);
  public void addRoot(Root root, boolean addSpecForClass);
  public String rootNameFor(FieldInfo fi);
  public boolean isRoot(FieldInfo fi);
  public boolean isRootDSOFinal(FieldInfo fi);
  
  public synchronized LockDefinition[] lockDefinitionsFor(MemberInfo memberInfo);
  
  public int getFaultCount();

  public void addWriteAutolock(String methodPattern);

  public void addSynchronousWriteAutolock(String methodPattern);
  
  public void addReadAutolock(String methodPattern);
  
  public synchronized void addAutolock(String methodPattern, ConfigLockLevel type);

  public void addReadAutoSynchronize(String methodPattern);

  public void addWriteAutoSynchronize(String methodPattern);
  
  public synchronized void addLock(String methodPattern, LockDefinition lockDefinition);

  public boolean shouldBeAdapted(ClassInfo classInfo);
  
  public boolean isNeverAdaptable(ClassInfo classInfo);

  public boolean isTransient(int modifiers, ClassInfo classInfo, String field);
  
  public boolean isVolatile(int modifiers, ClassInfo classInfo, String field);

  public boolean isCallConstructorOnLoad(ClassInfo classInfo);

  public String getPreCreateMethodIfDefined(String className);

  public String getPostCreateMethodIfDefined(String className);

  public String getOnLoadScriptIfDefined(ClassInfo classInfo);
  
  public String getOnLoadMethodIfDefined(ClassInfo classInfo);
  
  public Class getTCPeerClass(Class clazz);
  
  public boolean isDSOSessions(String name);

  public TransparencyClassAdapter createDsoClassAdapterFor(ClassVisitor writer, ClassInfo classInfo,
                                                           InstrumentationLogger lgr, ClassLoader caller,
                                                           final boolean forcePortable, boolean honorTransient);
  
  public ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                            ClassLoader caller);
  public ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                            ClassLoader caller, final boolean forcePortable);
  
  public TransparencyClassSpec getOrCreateSpec(String className);
  
  public TransparencyClassSpec getOrCreateSpec(final String className, final String applicator);

  public boolean isLogical(String className);

  public boolean isPortableModuleClass(Class clazz);

  public Class getChangeApplicator(Class clazz);
  
  public boolean isUseNonDefaultConstructor(Class clazz);

  public void setModuleSpecs(ModuleSpec[] moduleSpecs);
  
  public boolean hasSpec(String className);

   // This is used in BootJarTool. In BootJarTool, it changes the package of our implementation of ReentrantLock and
   // FutureTask to the java.util.concurrent package. In order to change the different adapter together, we need to
   // create a spec with our package and remove the spec after the instrumentation is done.
  public void removeSpec(String className);

  public TransparencyClassSpec getSpec(String className);
  
  // This method will: - check the contents of the boot-jar against tc-config.xml - check that all that all the
  // necessary referenced classes are also present in the boot jar
  public void verifyBootJarContents() throws UnverifiedBootJarException;

  public synchronized TransparencyClassSpec[] getAllSpecs();
 
  public void addDistributedMethodCall(DistributedMethodSpec dms);
 
  public DistributedMethodSpec getDmiSpec(MemberInfo memberInfo);

  public void addTransient(String className, String fieldName);

  public void writeTo(DSOApplicationConfigBuilder appConfigBuilder);
  
  public void addAspectModule(String pattern, String moduleName);

  public Map getAspectModules();
  
  public void addDSOSpringConfig(DSOSpringConfigHelper config);
  
  public Collection getDSOSpringConfigs();

  public String getLogicalExtendingClassName(String className);

  public void addApplicationName(String name);

  public void addSynchronousWriteApplication(String name);
  
  public void addUserDefinedBootSpec(String className, TransparencyClassSpec spec);

  public void addNewModule(String name, String version);
  
  public Modules getModulesForInitialization();
  
  public int getSessionLockType(String appName);
 */
}