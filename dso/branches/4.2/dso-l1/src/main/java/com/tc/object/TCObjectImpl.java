/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.util.Conversion;
import com.tc.util.Util;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of TCObject interface.
 */
public abstract class TCObjectImpl implements TCObject {
  private static final TCLogger logger                    = TCLogging.getLogger(TCObjectImpl.class);

  private static final int      ACCESSED_OFFSET           = 1 << 0;
  private static final int      IS_NEW_OFFSET             = 1 << 1;
  private static final int      AUTOLOCKS_DISABLED_OFFSET = 1 << 2;

  // This initial negative version number is important since GID is assigned in the server from 0.
  private long                  version                   = -1;

  private final ObjectID      objectID;
  private final TCClass         tcClazz;
  private WeakReference         peerObject;
  private byte                  flags                     = 0;

  protected TCObjectImpl(final ObjectID id, final Object peer, final TCClass clazz, final boolean isNew) {
    this.objectID = id;
    this.tcClazz = clazz;
    if (peer != null) {
      setPeerObject(getObjectManager().newWeakObjectReference(id, peer));
    }

    setFlag(IS_NEW_OFFSET, isNew);
  }

  @Override
  public boolean isShared() {
    return true;
  }

  public boolean isNull() {
    return this.peerObject == null || getPeerObject() == null;
  }

  @Override
  public ObjectID getObjectID() {
    return this.objectID;
  }

  protected ClientObjectManager getObjectManager() {
    return getTCClass().getObjectManager();
  }

  @Override
  public Object getPeerObject() {
    return this.peerObject == null ? null : this.peerObject.get();
  }

  protected void setPeerObject(final WeakReference pojo) {
    this.peerObject = pojo;
  }

  protected TCClass getTCClass() {
    return this.tcClazz;
  }

  @Override
  public void dehydrate(final DNAWriter writer) {
    getTCClass().dehydrate(this, writer, getPeerObject());
  }

  /**
   * Reconstitutes the object using the data in the DNA strand. XXX: We may need to signal (via a different signature or
   * args) that the hydration is intended to initialize the object from scratch or if it's a delta. We must avoid
   * creating a new instance of the peer object if the strand is just a delta.<br>
   * <p>
   * TODO:: Split into two interface, peer is null if not new.
   * 
   * @throws ClassNotFoundException
   */
  @Override
  public void hydrate(final DNA from, final boolean force, final WeakReference peer) throws ClassNotFoundException {
    synchronized (getResolveLock()) {
      if (peer != null) {
        setPeerObject(peer);
      }
      final Object po = getPeerObject();
      if (po == null) { return; }
      try {
        getTCClass().hydrate(this, from, po, force);
      } catch (final ClassNotFoundException e) {
        logger.warn("Re-throwing Exception: ", e);
        throw e;
      } catch (final IOException e) {
        logger.warn("Re-throwing Exception: ", e);
        throw new DNAException(e);
      }
    }
  }

  private synchronized void setFlag(final int offset, final boolean value) {
    this.flags = Conversion.setFlag(this.flags, offset, value);
  }

  private synchronized boolean getFlag(final int offset) {
    return Conversion.getFlag(this.flags, offset);
  }

  private synchronized boolean compareAndSetFlag(final int offset, final boolean old, final boolean newValue) {
    if (Conversion.getFlag(this.flags, offset) == old) {
      this.flags = Conversion.setFlag(this.flags, offset, newValue);
      return true;
    }
    return false;
  }

  @Override
  public ObjectID setReference(final String fieldName, final ObjectID id) {
    throw new AssertionError("shouldn't be called");
  }

  @Override
  public void setArrayReference(final int index, final ObjectID id) {
    throw new AssertionError("shouldn't be called");
  }

  @Override
  public void setValue(final String fieldName, final Object obj) {
    try {
      final TransparentAccess ta = (TransparentAccess) getPeerObject();
      if (ta == null) {
        // Object was GC'd so return which should lead to a re-retrieve
        return;
      }
      clearReference(fieldName);
      final TCField field = getTCClass().getField(fieldName);
      if (field == null) {
        logger.warn("Data for field:" + fieldName + " was recieved but that field does not exist in class:");
        return;
      }
      if (obj instanceof ObjectID) {
        setReference(fieldName, (ObjectID) obj);
        ta.__tc_setfield(field.getName(), null);
      } else {
        // clean this up
        ta.__tc_setfield(field.getName(), obj);
      }
    } catch (final Exception e) {
      logger.error("Error setting field [" + fieldName + "] to value of type " + typeOf(obj) + " on instance of "
                   + getTCClass().getPeerClass().getName() + " that has fields: " + fieldDesc());

      // TODO: More elegant exception handling.
      throw new com.tc.object.dna.api.DNAException(e);
    }
  }

  private String fieldDesc() {
    List<String> fields = new ArrayList<String>();
    Class c = getTCClass().getPeerClass();
    while (c != null) {
      for (Field f : c.getDeclaredFields()) {
        fields.add(c.getName() + "." + f.getName() + "(" + f.getType().getName() + ")");
      }
      c = c.getSuperclass();
    }
    return fields.toString();
  }

  private static String typeOf(Object obj) {
    if (obj == null) { return "null"; }
    return obj.getClass().getSimpleName();
  }

  @Override
  public final Object getResolveLock() {
    return this.objectID; // Save a field by using this one as the lock
  }

  @Override
  public void resolveArrayReference(final int index) {
    throw new AssertionError("shouldn't be called");
  }

  public void clearArrayReference(final int index) {
    clearReference(Integer.toString(index));
  }

  @Override
  public void clearReference(final String fieldName) {
    // do nothing
  }

  @Override
  public void resolveReference(final String fieldName) {
    // do nothing
  }

  @Override
  public void resolveAllReferences() {
    // override me
  }

  @Override
  public void literalValueChanged(final Object newValue, final Object oldValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setLiteralValue(final Object newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized void setVersion(final long version) {
    this.version = version;
  }

  @Override
  public synchronized long getVersion() {
    return this.version;
  }

  @Override
  public String toString() {
    return getClass().getName() + "@" + System.identityHashCode(this) + "[objectID=" + this.objectID + ", TCClass="
           + getTCClass() + "]";
  }

  @Override
  public void objectFieldChanged(final String classname, final String fieldname, final Object newValue, final int index) {
    try {
      markAccessed();
      if (index == NULL_INDEX) {
        // Assert.eval(fieldname.indexOf('.') >= 0);
        clearReference(fieldname);
      } else {
        clearArrayReference(index);
      }
      getObjectManager().getTransactionManager().fieldChanged(this, classname, fieldname, newValue, index);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  @Override
  public void objectFieldChangedByOffset(final String classname, final long fieldOffset, final Object newValue,
                                         final int index) {
    throw new AssertionError();
  }

  public boolean isFieldPortableByOffset(final long fieldOffset) {
    return getTCClass().isPortableField(fieldOffset);
  }

  @Override
  public String getFieldNameByOffset(final long fieldOffset) {
    throw new AssertionError();
  }

  @Override
  public void booleanFieldChanged(final String classname, final String fieldname, final boolean newValue,
                                  final int index) {
    objectFieldChanged(classname, fieldname, Boolean.valueOf(newValue), index);
  }

  @Override
  public void byteFieldChanged(final String classname, final String fieldname, final byte newValue, final int index) {
    objectFieldChanged(classname, fieldname, Byte.valueOf(newValue), index);
  }

  @Override
  public void charFieldChanged(final String classname, final String fieldname, final char newValue, final int index) {
    objectFieldChanged(classname, fieldname, Character.valueOf(newValue), index);
  }

  @Override
  public void doubleFieldChanged(final String classname, final String fieldname, final double newValue, final int index) {
    objectFieldChanged(classname, fieldname, Double.valueOf(newValue), index);
  }

  @Override
  public void floatFieldChanged(final String classname, final String fieldname, final float newValue, final int index) {
    objectFieldChanged(classname, fieldname, Float.valueOf(newValue), index);
  }

  @Override
  public void intFieldChanged(final String classname, final String fieldname, final int newValue, final int index) {
    objectFieldChanged(classname, fieldname, Integer.valueOf(newValue), index);
  }

  @Override
  public void longFieldChanged(final String classname, final String fieldname, final long newValue, final int index) {
    objectFieldChanged(classname, fieldname, Long.valueOf(newValue), index);
  }

  @Override
  public void shortFieldChanged(final String classname, final String fieldname, final short newValue, final int index) {
    objectFieldChanged(classname, fieldname, Short.valueOf(newValue), index);
  }

  @Override
  public void objectArrayChanged(final int startPos, final Object[] array, final int length) {
    markAccessed();
    for (int i = 0; i < length; i++) {
      clearArrayReference(startPos + i);
    }
    getObjectManager().getTransactionManager().arrayChanged(this, startPos, array, length);
  }

  @Override
  public void primitiveArrayChanged(final int startPos, final Object array, final int length) {
    markAccessed();
    getObjectManager().getTransactionManager().arrayChanged(this, startPos, array, length);
  }

  @Override
  public void markAccessed() {
    setFlag(ACCESSED_OFFSET, true);
  }

  @Override
  public void clearAccessed() {
    setFlag(ACCESSED_OFFSET, false);
  }

  @Override
  public boolean recentlyAccessed() {
    return getFlag(ACCESSED_OFFSET);
  }

  @Override
  public int accessCount(final int factor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNew() {
    return getFlag(IS_NEW_OFFSET);
  }

  @Override
  public void setNotNew() {
    // Flipping the "new" flag must occur AFTER dehydrate -- otherwise the client
    // memory manager might start nulling field values! (see canEvict() dependency on isNew() condition)
    if (!compareAndSetFlag(IS_NEW_OFFSET, true, false)) { throw new AssertionError(this + " : Already not new"); }
  }

  // These autlocking disable methods are checked in ManagerImpl. The one known use case
  // is the Hashtable used to hold sessions. We need local synchronization,
  // but we don't ever want autolocks for that particular instance
  @Override
  public void disableAutoLocking() {
    setFlag(AUTOLOCKS_DISABLED_OFFSET, true);
  }

  @Override
  public boolean autoLockingDisabled() {
    return getFlag(AUTOLOCKS_DISABLED_OFFSET);
  }

  @Override
  public String getExtendingClassName() {
    return getTCClass().getExtendingClassName();
  }

  @Override
  public String getClassName() {
    return getTCClass().getName();
  }

  @Override
  public Class<?> getPeerClass() {
    return getTCClass().getPeerClass();
  }

  @Override
  public boolean isIndexed() {
    return getTCClass().isIndexed();
  }

  @Override
  public boolean isLogical() {
    return getTCClass().isLogical();
  }

  @Override
  public boolean isEnum() {
    return getTCClass().isEnum();
  }
}
