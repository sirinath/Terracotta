package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

// XXX: This is a rather ugly hack to get around the requirements of tim-concurrent-collections.
public class ConcurrentDistributedMapManagedObjectState extends PartialMapManagedObjectState {
  public static final String DSO_LOCK_TYPE_FIELDNAME = "dsoLockType";
  public static final String LOCK_STRATEGY_FIELDNAME = "lockStrategy";

  private int                dsoLockType;
  private ObjectID           lockStrategy;

  private ConcurrentDistributedMapManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  protected ConcurrentDistributedMapManagedObjectState(long classId, Map map) {
    super(classId, map);
  }

  @Override
  public byte getType() {
    return CONCURRENT_DISTRIBUTED_MAP_TYPE;
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    super.addAllObjectReferencesTo(refs);
    if (!lockStrategy.isNull()) {
      refs.add(lockStrategy);
    }
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        PhysicalAction physicalAction = (PhysicalAction) action;

        String fieldName = physicalAction.getFieldName();
        if (fieldName.equals(DSO_LOCK_TYPE_FIELDNAME)) {
          dsoLockType = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(LOCK_STRATEGY_FIELDNAME)) {
          ObjectID newLockStrategy = (ObjectID) physicalAction.getObject();
          getListener().changed(objectID, lockStrategy, newLockStrategy);
          lockStrategy = newLockStrategy;
        } else {
          throw new AssertionError("unexpected field name: " + fieldName);
        }
      } else {
        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        applyMethod(objectID, includeIDs, method, params);
      }
    }
  }

   @Override
  protected void addPruneConditions(final BackReferences includeIDs, final Object old, final ObjectID objectID) {
    if (old instanceof ObjectID) {
      includeIDs.addConditionsForBroadcast(objectID, (ObjectID) old);
    } else if (old == null) {
      // Dont broadcast
      includeIDs.addConditionsForBroadcast(objectID, ObjectID.NULL_ID);
    }
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    if (!this.lockStrategy.isNull()) {
      traverser.addRequiredObjectIDs(Collections.singleton(this.lockStrategy));
    }
    traverser.addReachableObjectIDs(getObjectReferencesFrom(this.references.keySet()));
    traverser.addReachableObjectIDs(getObjectReferencesFrom(this.references.values()));
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeInt(dsoLockType);
    out.writeLong(lockStrategy.getObjectID());
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    writer.addPhysicalAction(DSO_LOCK_TYPE_FIELDNAME, Integer.valueOf(dsoLockType));
    writer.addPhysicalAction(LOCK_STRATEGY_FIELDNAME, lockStrategy);
    super.dehydrate(objectID, writer);
  }

  static MapManagedObjectState readFrom(ObjectInput in) throws IOException {
    ConcurrentDistributedMapManagedObjectState cdmMos = new ConcurrentDistributedMapManagedObjectState(in);
    cdmMos.dsoLockType = in.readInt();
    cdmMos.lockStrategy = new ObjectID(in.readLong());
    return cdmMos;
  }

  public Object getValueForKey(final Object portableKey) {
    return this.references.get(portableKey);
  }

}
