/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class RootMapManagedObjectState extends AbstractManagedObjectState {
  private final long           classID;
  private volatile Object[]    array;

  private static final boolean DEBUG = true;

  public RootMapManagedObjectState(final long classID) {
    this.classID = classID;
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo applyInfo) throws IOException {
    while (cursor.next()) {
      LogicalAction action = cursor.getLogicalAction();
      if (action.getMethod() == SerializationUtil.CREATE_ARRAY) {
        Object[] parameters = action.getParameters();
        this.array = parameters.clone();
        if (DEBUG) {
          System.err.println("RootMapManagedObjectState: applied " + Arrays.toString(this.array));
        }
      } else {
        throw new AssertionError("Illegal State sent from client");
      }
    }
  }

  @Override
  public Set getObjectReferences() {
    if (array == null) { return Collections.EMPTY_SET; }
    final ObjectIDSet refs = new ObjectIDSet();
    for (Object o : array) {
      if (o instanceof ObjectID) {
        refs.add((ObjectID) o);
      }
    }
    return refs;
  }

  @Override
  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    traverser.addRequiredObjectIDs(getObjectReferences());
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    Object[] cloned = array.clone();
    writer.addLogicalAction(SerializationUtil.CREATE_ARRAY, cloned);
  }

  @Override
  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    final int size = array.length;

    if (limit < 0) {
      limit = size;
    } else {
      limit = Math.min(limit, size);
    }

    Object[] data = new Object[limit];

    for (int index = 0; index < size && index < limit; index++) {
      data[index] = array[index];
    }

    return LogicalManagedObjectFacade.createListInstance(objectID, className, data, size);
  }

  @Override
  public byte getType() {
    return ManagedObjectState.ROOT_MAP_TYPE;
  }

  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  // public final String getLoaderDescription() {
  // return getStateFactory().getLoaderDescription(this.classID);
  // }

  @Override
  protected boolean basicEquals(AbstractManagedObjectState o) {
    RootMapManagedObjectState mo = (RootMapManagedObjectState) o;
    return Arrays.equals(mo.array, this.array);
  }

  @Override
  public void writeTo(ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    out.writeInt(array.length);
    for (Object obj : array) {
      out.writeObject(obj);
    }
  }

  public static ManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    RootMapManagedObjectState rootMo = new RootMapManagedObjectState(in.readLong());
    int size = in.readInt();
    Object[] tempArray = new Object[size];
    for (int i = 0; i < size; i++) {
      tempArray[i] = in.readObject();
    }
    rootMo.array = tempArray;
    return rootMo;
  }

}
