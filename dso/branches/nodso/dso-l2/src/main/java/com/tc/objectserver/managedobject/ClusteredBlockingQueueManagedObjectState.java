/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClusteredBlockingQueueManagedObjectState extends AbstractManagedObjectState {

  public static final String  CBQ_TOOLKIT_CLASSNAME   = "com.terracotta.toolkit2.collections.ClusteredBlockingQueueImpl";

  private static final String BACKING_LIST_FIELD_NAME = "backingList";
  private static final String NAME_FIELD_NAME         = "name";
  private static final String CAPACITY_FIELD_NAME     = "capacity";

  private final long          classID;

  private String              name;
  private int                 capacity;
  private ObjectID            backingListObjectId;

  public ClusteredBlockingQueueManagedObjectState(final long classID) {
    this.classID = classID;
  }

  public ClusteredBlockingQueueManagedObjectState(ObjectInput in) throws IOException {
    this.classID = in.readLong();
    this.name = in.readUTF();
    this.capacity = in.readInt();
    this.backingListObjectId = new ObjectID(in.readLong());
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final ClusteredBlockingQueueManagedObjectState other = (ClusteredBlockingQueueManagedObjectState) o;
    return name.equals(other.name) && capacity == other.capacity
           && backingListObjectId.equals(other.backingListObjectId);
  }

  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    final Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>());
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(final Map<String, Object> fields) {
    fields.put(NAME_FIELD_NAME, name);
    fields.put(CAPACITY_FIELD_NAME, capacity);
    return fields;
  }

  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  public final String getLoaderDescription() {
    return getStateFactory().getLoaderDescription(this.classID);
  }

  public Set getObjectReferences() {
    ObjectIDSet refs = new ObjectIDSet();
    if (!backingListObjectId.isNull()) {
      refs.add(backingListObjectId);
    }
    return refs;
  }

  public byte getType() {
    return ManagedObjectState.CLUSTERED_BLOCKING_QUEUE_TYPE;
  }

  static ClusteredBlockingQueueManagedObjectState readFrom(final ObjectInput in) throws IOException {
    return new ClusteredBlockingQueueManagedObjectState(in);
  }

  public void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    out.writeUTF(name);
    out.writeInt(capacity);
    out.writeLong(backingListObjectId.toLong());
  }

  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    writer.addPhysicalAction(NAME_FIELD_NAME, name);
    writer.addPhysicalAction(CAPACITY_FIELD_NAME, Integer.valueOf(capacity));
    writer.addPhysicalAction(BACKING_LIST_FIELD_NAME, backingListObjectId);
  }

  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs)
      throws IOException {
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        final PhysicalAction physicalAction = (PhysicalAction) action;

        final String fieldName = physicalAction.getFieldName();
        if (fieldName.equals(NAME_FIELD_NAME)) {
          Object value = physicalAction.getObject();
          if (value instanceof UTF8ByteDataHolder) {
            this.name = ((UTF8ByteDataHolder) value).asString();
          } else {
            this.name = (String) value;
          }
        } else if (fieldName.equals(CAPACITY_FIELD_NAME)) {
          this.capacity = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(BACKING_LIST_FIELD_NAME)) {
          final ObjectID newBackingListObjectId = (ObjectID) physicalAction.getObject();
          getListener().changed(objectID, this.backingListObjectId, newBackingListObjectId);
          this.backingListObjectId = newBackingListObjectId;
        } else {
          throw new AssertionError("unexpected field name: " + fieldName + ", value: " + physicalAction.getObject());
        }
      } else {
        LogicalAction la = (LogicalAction) action;
        throw new AssertionError("unexpected logical action: method: " + la.getMethod() + ", params: "
                                 + Arrays.asList(la.getParameters()));
      }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((backingListObjectId == null) ? 0 : backingListObjectId.hashCode());
    result = prime * result + capacity;
    result = prime * result + (int) (classID ^ (classID >>> 32));
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

}
