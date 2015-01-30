/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.impl.ProxyInstance;
import com.tc.object.field.TCField;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.loaders.Namespace;
import com.tc.util.ClassUtils;
import com.tc.util.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Peer of a Class under management.
 * <p>
 * This is used to cache the fields of each class by type.
 * 
 * @author orion
 */
public class TCClassImpl implements TCClass {
  private final static TCLogger          logger                 = TCLogging.getLogger(TCClassImpl.class);
  private final static Pattern           PARENT_FIELD_PATTERN   = Pattern.compile("^this\\$\\d+$");

  /**
   * Peer java class that this TCClass represents.
   */
  private final Class                    peer;

  private final TCClass                  superclazz;
  private final TCClassFactory           clazzFactory;
  private final TCField[]                portableFields;
  private final boolean                  indexed;
  private final boolean                  isNonStaticInner;
  private final boolean                  isLogical;
  private final ChangeApplicator         applicator;
  private final String                   parentFieldName;
  private final Map                      declaredTCFieldsByName = new HashMap();
  private final Map                      tcFieldsByName         = new HashMap();
  private final Field                    parentField;
  private final boolean                  useNonDefaultConstructor;
  private final ClientObjectManager      objectManager;
  private final boolean                  isProxyClass;
  private final boolean                  isEnum;
  private final String                   logicalExtendingClassName;
  private final Class                    logicalSuperClass;
  private final boolean                  useResolveLockWhileClearing;
  private final List<Method>             postCreateMethods;
  private final List<Method>             preCreateMethods;
  private Constructor                    constructor            = null;

  TCClassImpl(final TCFieldFactory factory, final TCClassFactory clazzFactory, final ClientObjectManager objectManager,
              final Class peer, final Class logicalSuperClass, final String logicalExtendingClassName,
              final boolean isLogical, final boolean useNonDefaultConstructor,
              final boolean useResolveLockWhileClearing, final String postCreateMethod, final String preCreateMethod) {
    this.clazzFactory = clazzFactory;
    this.objectManager = objectManager;
    this.peer = peer;
    this.indexed = peer.isArray();

    final boolean isStatic = Modifier.isStatic(peer.getModifiers());
    final boolean mightBeInner = peer.getName().indexOf('$') != -1 && !isIndexed();
    this.parentField = mightBeInner && !isStatic ? findParentField() : null;
    this.isNonStaticInner = this.parentField != null;
    this.parentFieldName = this.parentField == null ? null : getName() + '.' + this.parentField.getName();

    this.isLogical = isLogical;
    this.isProxyClass = Proxy.isProxyClass(peer) || ProxyInstance.class.getName().equals(peer.getName());
    this.superclazz = findSuperClass(peer);
    this.isEnum = ClassUtils.isDsoEnum(peer);
    this.logicalExtendingClassName = logicalExtendingClassName;

    this.applicator = createApplicator();

    introspectFields(peer, factory);
    this.portableFields = createPortableFields();
    this.useNonDefaultConstructor = this.isProxyClass || useNonDefaultConstructor;
    this.logicalSuperClass = logicalSuperClass;
    this.useResolveLockWhileClearing = useResolveLockWhileClearing;
    this.postCreateMethods = resolveCreateMethods(postCreateMethod, false);
    this.preCreateMethods = resolveCreateMethods(preCreateMethod, true);
  }

  private List<Method> resolveCreateMethods(final String methodName, final boolean preCreate) {
    final List<Method> rv = new ArrayList<Method>();
    if (this.superclazz != null) {
      rv.addAll(preCreate ? this.superclazz.getPreCreateMethods() : this.superclazz.getPostCreateMethods());
    }

    if (methodName != null) {
      try {
        final Method method = this.peer.getDeclaredMethod(methodName);
        method.setAccessible(true);
        rv.add(method);
      } catch (final Exception e) {
        logger.error("Exception resolving method '" + methodName + "' on " + this.peer, e);
      }
    }

    if (rv.isEmpty()) { return Collections.EMPTY_LIST; }

    return Collections.unmodifiableList(rv);
  }

  @Override
  public Field getParentField() {
    return this.parentField;
  }

  @Override
  public boolean isNonStaticInner() {
    return this.isNonStaticInner;
  }

  @Override
  public Class getPeerClass() {
    return this.peer;
  }

  private Field findParentField() {
    final Field[] fields = this.peer.getDeclaredFields();
    for (final Field field : fields) {
      if (PARENT_FIELD_PATTERN.matcher(field.getName()).matches()) { return field; }
    }
    return null;
  }

  private TCClass findSuperClass(final Class c) {
    final Class superclass = c.getSuperclass();
    if (superclass != null) { return this.clazzFactory.getOrCreate(superclass, this.objectManager); }
    return null;
  }

  private ChangeApplicator createApplicator() {
    return this.clazzFactory.createApplicatorFor(this);
  }

  @Override
  public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force)
      throws IOException, ClassNotFoundException {
    // Okay...long story here The application of the DNA used to be a synchronized(applicator) block. As best as Steve
    // and I could tell, the synchronization was solely a memory boundary and not a mutual exlusion mechanism. For the
    // time being, we have resolved that we need no synchronization here (either for memory, or exclusion). The memory
    // barrier aspect isn't known to be a problem and the concurrency is handled by the server (ie. we won't get
    // concurrent updates). At some point it would be a good idea to detect (and error out) when updates are received
    // from L2 but local read/writes have been made on the target TCObject

    final long localVersion = tcObject.getVersion();
    final long dnaVersion = dna.getVersion();

    if (force || (localVersion < dnaVersion)) {
      tcObject.setVersion(dnaVersion);
      this.applicator.hydrate(this.objectManager, tcObject, dna, pojo);
    } else if (logger.isDebugEnabled()) {
      logger
          .debug("IGNORING UPDATE, local object at version " + localVersion + ", dna update is version " + dnaVersion);
    }

  }

  @Override
  public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
    try {
      this.applicator.dehydrate(this.objectManager, tcObject, writer, pojo);
    } catch (final ConcurrentModificationException cme) {
      // try to log some useful stuff about the pojo in question here.
      // This indicates improper locking, but is certainly possible
      final String type = pojo == null ? "null" : pojo.getClass().getName();
      final String toString = String.valueOf(pojo);
      final int ihc = System.identityHashCode(pojo);
      logger.error("Shared object (presumably new) modified during dehydrate (type " + type + ", ihc " + ihc + "): "
                   + toString, cme);
      throw cme;
    }
  }

  @Override
  public Class getComponentType() {
    return this.peer.getComponentType();
  }

  @Override
  public boolean isEnum() {
    return this.isEnum;
  }

  @Override
  public String getName() {
    if (this.isProxyClass) { return ProxyInstance.class.getName(); }
    if (this.isEnum) { return LiteralValues.ENUM_CLASS_DOTS; }
    return this.peer.getName();
  }

  @Override
  public String getExtendingClassName() {
    String className = getName();
    if (this.logicalExtendingClassName != null) {
      className = Namespace.createLogicalExtendingClassName(className, this.logicalExtendingClassName);
    }
    return className;
  }

  @Override
  public TCClass getSuperclass() {
    return this.superclazz;
  }

  @Override
  public synchronized Constructor getConstructor() {
    if (this.constructor == null) {
      // As best as I can tell, the reason for the lazy initialization here is that we don't actually need the cstr
      // looked up for all of the TCClass instances we cook up. Additionally, the assertions in findConstructor will go
      // off for a fair number of abstract base classes (eg. java.util.AbstractMap, java.util.Dictionary, etc)
      this.constructor = findConstructor();
    }
    return this.constructor;
  }

  private Constructor findConstructor() {
    Constructor rv = null;

    if (this.isLogical) {
      final Constructor[] cons = this.peer.getDeclaredConstructors();
      for (final Constructor con : cons) {
        final Class[] types = con.getParameterTypes();
        if (types.length == 0) {
          rv = con;
          rv.setAccessible(true);
          return rv;
        }
      }
    }

    rv = ReflectionUtil.newConstructor(this.peer, this.logicalSuperClass);
    rv.setAccessible(true);
    return rv;
  }

  @Override
  public String getParentFieldName() {
    return this.parentFieldName;
  }

  private void introspectFields(final Class clazz, final TCFieldFactory fieldFactory) {
    // Note: this gets us all of the fields declared in the class, static
    // as well as instance fields.
    final Field[] fields = clazz.equals(Object.class) ? new Field[0] : clazz.getDeclaredFields();

    Field field;
    TCField tcField;
    for (final Field field2 : fields) {
      field = field2;
      // The factory does a bunch of callbacks based on the field type.
      tcField = fieldFactory.getInstance(this, field);
      this.declaredTCFieldsByName.put(field.getName(), tcField);
      this.tcFieldsByName.put(tcField.getName(), tcField);
    }
  }

  @Override
  public String toString() {
    return this.peer.getName();
  }

  /**
   * Expects the field name in the format <classname>. <fieldname>(e.g. com.foo.Bar.baz)
   */
  @Override
  public TCField getField(final String name) {
    TCField rv = (TCField) this.tcFieldsByName.get(name);
    if (rv == null && this.superclazz != null) {
      rv = this.superclazz.getField(name);
    }
    return rv;
  }

  @Override
  public TCField[] getPortableFields() {
    return this.portableFields;
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    return this.applicator.getPortableObjects(pojo, addTo);
  }

  private TCField[] createPortableFields() {
    if (this.isLogical || !this.objectManager.isPortableClass(this.peer)) { return new TCField[0]; }
    final LinkedList l = new LinkedList();

    for (final Iterator i = this.declaredTCFieldsByName.values().iterator(); i.hasNext();) {

      final TCField f = (TCField) i.next();
      if (f.isPortable()) {
        l.add(f);
      }
    }

    return (TCField[]) l.toArray(new TCField[l.size()]);
  }

  @Override
  public boolean isIndexed() {
    return this.indexed;
  }

  @Override
  public boolean isLogical() {
    return this.isLogical;
  }

  @Override
  public ClientObjectManager getObjectManager() {
    return this.objectManager;
  }

  @Override
  public TCObject createTCObject(final ObjectID id, final Object pojo, final boolean isNew) {
    if (pojo instanceof TCObjectSelf) {
      TCObjectSelf self = (TCObjectSelf) pojo;
      self.initializeTCObject(id, this, isNew);
      return self;
    } else if (this.isLogical) {
      return new TCObjectLogical(id, pojo, this, isNew);
    } else {
      return new TCObjectPhysical(id, pojo, this, isNew);
    }
  }

  @Override
  public boolean isUseNonDefaultConstructor() {
    return this.useNonDefaultConstructor;
  }

  @Override
  public Object getNewInstanceFromNonDefaultConstructor(final DNA dna) throws IOException, ClassNotFoundException {
    final Object o = this.applicator.getNewInstance(this.objectManager, dna);

    if (o == null) { throw new AssertionError("Can't find suitable constructor for class: " + getName() + "."); }
    return o;
  }

  @Override
  public boolean isPortableField(final long fieldOffset) {
    throw new AssertionError();
  }

  @Override
  public boolean isProxyClass() {
    return this.isProxyClass;
  }

  @Override
  public boolean useResolveLockWhileClearing() {
    return this.useResolveLockWhileClearing;
  }

  @Override
  public List<Method> getPostCreateMethods() {
    return this.postCreateMethods;
  }

  @Override
  public List<Method> getPreCreateMethods() {
    return this.preCreateMethods;
  }
}
