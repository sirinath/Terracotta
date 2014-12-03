/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public abstract class AbstractLocalCacheStoreValue implements Externalizable {
  /**
   * This corresponds to a ObjectID/LockID
   */
  protected volatile Object id;
  /**
   * this is the value object <br>
   * TODO: make this Serializable. This would be a SerializedEntry for the serialized caches.
   */
  protected volatile Object value;

  public AbstractLocalCacheStoreValue() {
    //
  }

  public AbstractLocalCacheStoreValue(Object id, Object value) {
    this.id = id;
    this.value = value;
  }

  public Object getMetaId() {
    return id;
  }

  public final Object getValueObject() {
    return value;
  }

  public boolean isLiteral() {
    return this.value != null && getValueObjectId().equals(ObjectID.NULL_ID);
  }

  public boolean isValueNull() {
    return value == null;
  }

  /**
   * Returns true if this is cached value for eventual consistency
   */
  public boolean isEventualConsistentValue() {
    return false;
  }

  /**
   * Returns true if this is cached value for incoherent/bulk-load
   */
  public boolean isIncoherentValue() {
    return false;
  }

  /**
   * Returns true if this is cached value for strong consistency
   */
  public boolean isStrongConsistentValue() {
    return false;
  }

  /**
   * Returns this object as {@link LocalCacheStoreStrongValue}. Use only when {@link #isStrongConsistentValue()} is
   * true, otherwise will throw ClassCastException
   */
  public LocalCacheStoreStrongValue asStrongValue() {
    return (LocalCacheStoreStrongValue) this;
  }

  /**
   * Returns this object as {@link LocalCacheStoreEventualValue}. Use only when {@link #isEventualConsistentValue()} is
   * true, otherwise will throw ClassCastException
   */
  public LocalCacheStoreEventualValue asEventualValue() {
    return (LocalCacheStoreEventualValue) this;
  }

  /**
   * Use only when {@link #isStrongConsistentValue()} is true. Returns the lock Id
   */
  public LockID getLockId() {
    throw new UnsupportedOperationException("This should only be called for Strong consistent cached values");
  }

  public abstract ObjectID getValueObjectId();

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(id);
    out.writeObject(value);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    id = in.readObject();
    value = in.readObject();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AbstractLocalCacheStoreValue other = (AbstractLocalCacheStoreValue) obj;
    if (id == null) {
      if (other.id != null) return false;
    } else if (!id.equals(other.id)) return false;
    if (value == null) {
      if (other.value != null) return false;
    } else if (!value.equals(other.value)) return false;
    return true;
  }
}
