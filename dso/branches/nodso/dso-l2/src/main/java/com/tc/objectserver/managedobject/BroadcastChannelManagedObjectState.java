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
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BroadcastChannelManagedObjectState extends AbstractManagedObjectState {

  private static final String   NAME_FIELD                          = "name";

  private static final TCLogger logger                              = TCLogging
                                                                        .getLogger(BroadcastChannelManagedObjectState.class);

  public static final String    BROADCAST_CHANNEL_TOOLKIT_CLASSNAME = "com.terracotta.toolkit.util.BroadcastChannelImpl";

  private final long            classID;

  private String                name;

  public BroadcastChannelManagedObjectState(final long classID) {
    this.classID = classID;
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final BroadcastChannelManagedObjectState other = (BroadcastChannelManagedObjectState) o;
    return name.equals(other.name);
  }

  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs)
      throws IOException {
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        PhysicalAction pa = (PhysicalAction) action;
        if (pa.getFieldName().equals(NAME_FIELD)) {
          Object value = pa.getObject();
          if (value instanceof UTF8ByteDataHolder) {
            this.name = ((UTF8ByteDataHolder) value).asString();
          } else {
            this.name = (String) value;
          }
        } else {
          logger.error("received physical action: fieldName: " + pa.getFieldName() + ", object: " + pa.getObject()
                       + " -- ignoring it");
        }
      } else if (action instanceof LogicalAction) {
        LogicalAction la = (LogicalAction) action;
        // TODO: remove this logging
        logger.info("XXX: Remove me :: Got logical action: method: " + la.getMethod() + ", params: "
                    + Arrays.asList(la.getParameters()));
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
    return true;
  }

  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    final Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>());
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(final Map<String, Object> fields) {
    fields.put(NAME_FIELD, name);
    return fields;
  }

  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    writer.addPhysicalAction(NAME_FIELD, name);
  }

  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  public final String getLoaderDescription() {
    return getStateFactory().getLoaderDescription(this.classID);
  }

  public Set getObjectReferences() {
    return Collections.EMPTY_SET;
  }

  public byte getType() {
    return ManagedObjectState.BROADCAST_CHANNEL_TYPE;
  }

  public void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    out.writeUTF(name);
  }

  static BroadcastChannelManagedObjectState readFrom(final ObjectInput in) throws IOException {
    final BroadcastChannelManagedObjectState state = new BroadcastChannelManagedObjectState(in.readLong());
    state.readFromInternal(in);
    return state;
  }

  protected void readFromInternal(final ObjectInput in) throws IOException {
    name = in.readUTF();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (classID ^ (classID >>> 32));
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }
}
