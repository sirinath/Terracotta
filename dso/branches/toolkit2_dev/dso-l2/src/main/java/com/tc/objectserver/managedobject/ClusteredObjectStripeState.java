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
  private ObjectID[]            componentObjectIds;

  public ClusteredObjectStripeState(final long classID) {
    this.classID = classID;
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final ClusteredObjectStripeState other = (ClusteredObjectStripeState) o;

    if (configurationObjectId != other.configurationObjectId) { return false; }
    if (!Arrays.equals(this.componentObjectIds, other.componentObjectIds)) { return false; }

    return true;
  }

  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs)
      throws IOException {
    while (cursor.next()) {
      final PhysicalAction pa = cursor.getPhysicalAction();
      if (pa.getFieldName().equals(CONFIGURATION_FIELD_NAME)) {
        ObjectID newOid = (ObjectID) pa.getObject();
        getListener().changed(objectID, configurationObjectId, newOid);
        this.configurationObjectId = newOid;
      } else if (pa.isEntireArray()) {
        final Object array = pa.getObject();
        if (array instanceof ObjectID[]) {
          this.componentObjectIds = (ObjectID[]) array;
        } else {
          final String type = safeTypeName(array);
          logger.error("received array of type " + type + " -- ignoring it");
        }
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

  private static String safeTypeName(final Object obj) {
    final String type = obj == null ? "null" : obj.getClass().getName();
    return type;
  }

  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    final Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>());
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(final Map<String, Object> fields) {
    // TODO: implement this
    return fields;
  }

  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    writer.addPhysicalAction(CONFIGURATION_FIELD_NAME, configurationObjectId);
    writer.addEntireArray(componentObjectIds);
  }

  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  public Set getObjectReferences() {
    ObjectIDSet set = new ObjectIDSet();
    if (configurationObjectId != null && !configurationObjectId.isNull()) {
      set.add(configurationObjectId);
    }
    if (componentObjectIds != null) {
      for (ObjectID oid : componentObjectIds) {
        if (oid != null && !oid.isNull()) {
          set.add(oid);
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
    if (this.componentObjectIds != null) {
      out.writeInt(this.componentObjectIds.length);
      for (ObjectID oid : componentObjectIds) {
        out.writeLong(oid.toLong());
      }
    } else {
      out.writeInt(-1);
    }
  }

  static ClusteredObjectStripeState readFrom(final ObjectInput in) throws IOException {
    final ClusteredObjectStripeState state = new ClusteredObjectStripeState(in.readLong());
    state.readFromInternal(in);
    return state;
  }

  protected void readFromInternal(final ObjectInput in) throws IOException {
    configurationObjectId = new ObjectID(in.readLong());
    final int length = in.readInt();
    if (length >= 0) {
      componentObjectIds = new ObjectID[length];
      for (int i = 0; i < componentObjectIds.length; i++) {
        componentObjectIds[i] = new ObjectID(in.readLong());
      }
    } else {
      componentObjectIds = new ObjectID[0];
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (classID ^ (classID >>> 32));
    result = prime * result + Arrays.hashCode(componentObjectIds);
    result = prime * result + ((configurationObjectId == null) ? 0 : configurationObjectId.hashCode());
    return result;
  }

}