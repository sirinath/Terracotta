/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
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

public class ClusteredObjectStripeState extends AbstractManagedObjectState {

  private static final TCLogger logger                   = TCLogging.getLogger(ClusteredObjectStripeState.class);

  private static final String   CONFIGURATION_FIELD_NAME = "configuration";

  private final long            classID;

  private ObjectID              configurationObjectId;
  private Object[]              componentObjects;

  public ClusteredObjectStripeState(final long classID) {
    this.classID = classID;
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final ClusteredObjectStripeState other = (ClusteredObjectStripeState) o;

    if (configurationObjectId != other.configurationObjectId) { return false; }
    if (!Arrays.equals(this.componentObjects, other.componentObjects)) { return false; }

    return true;
  }

  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs)
      throws IOException {
    while (cursor.next()) {
      final PhysicalAction pa = cursor.getPhysicalAction();
      if (pa.isEntireArray()) {
        this.componentObjects = (Object[]) pa.getObject();
      } else if (CONFIGURATION_FIELD_NAME.equals(pa.getFieldName())) {
        ObjectID newOid = (ObjectID) pa.getObject();
        getListener().changed(objectID, configurationObjectId, newOid);
        this.configurationObjectId = newOid;
      } else {
        logger.error("received physical action: " + pa + " -- ignoring it");
      }
    }
  }

  /**
   * This method returns whether this ManagedObjectState can have references or not. @ return true : The Managed object
   * represented by this state object will never have any reference to other objects. false : The Managed object
   * represented by this state object can have references to other objects.
   */
  @Override
  public boolean hasNoReferences() {
    return false;
  }

  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    final Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>(), limit);
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(final Map<String, Object> fields, int limit) {
    fields.put(CONFIGURATION_FIELD_NAME, configurationObjectId);

    if (componentObjects != null) {
      int size = componentObjects.length;
      if (limit < 0) {
        limit = size;
      } else {
        limit = Math.min(limit, size);
      }
      for (int i = 0; i < limit; i++) {
        fields.put("components[" + i + "/" + componentObjects.length + "]", componentObjects[i]);
      }
    } else {
      fields.put("components", "<Empty Array>");
    }
    return fields;
  }

  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    writer.addPhysicalAction(CONFIGURATION_FIELD_NAME, configurationObjectId);
    writer.addEntireArray(componentObjects);
  }

  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  public Set getObjectReferences() {
    ObjectIDSet set = new ObjectIDSet();
    if (configurationObjectId != null && !configurationObjectId.isNull()) {
      set.add(configurationObjectId);
    }
    if (componentObjects != null) {
      for (Object obj : componentObjects) {
        if (obj instanceof ObjectID) {
          ObjectID oid = (ObjectID) obj;
          if (!oid.isNull()) {
            set.add(oid);
          }
        }
      }
    }
    return set;
  }

  public byte getType() {
    return ManagedObjectStateStaticConfig.CLUSTERED_OBJECT_STRIPE.getStateObjectType();
  }

  public void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    out.writeLong(configurationObjectId.toLong());
    if (this.componentObjects != null) {
      out.writeInt(this.componentObjects.length);
      for (Object obj : componentObjects) {
        out.writeObject(obj);
      }
    } else {
      out.writeInt(-1);
    }
  }

  static ClusteredObjectStripeState readFrom(final ObjectInput in) throws IOException, ClassNotFoundException {
    final ClusteredObjectStripeState state = new ClusteredObjectStripeState(in.readLong());
    state.readFromInternal(in);
    return state;
  }

  protected void readFromInternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    configurationObjectId = new ObjectID(in.readLong());
    final int length = in.readInt();
    if (length >= 0) {
      componentObjects = new Object[length];
      for (int i = 0; i < componentObjects.length; i++) {
        componentObjects[i] = in.readObject();
      }
    } else {
      componentObjects = new Object[0];
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (classID ^ (classID >>> 32));
    result = prime * result + Arrays.hashCode(componentObjects);
    result = prime * result + ((configurationObjectId == null) ? 0 : configurationObjectId.hashCode());
    return result;
  }

}