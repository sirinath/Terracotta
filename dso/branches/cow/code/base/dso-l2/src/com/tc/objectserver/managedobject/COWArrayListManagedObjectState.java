/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Server representation of a list
 */
public class COWArrayListManagedObjectState extends ListManagedObjectState {
  private static final String LOCK_FIELD_NAME = "java.util.concurrent.CopyOnWriteArrayList.lock";

  private ObjectID            lockField;

  COWArrayListManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  protected COWArrayListManagedObjectState(long classID) {
    super(classID);
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        applyOperation(method, objectID, includeIDs, params);
      } else if (action instanceof PhysicalAction) {
        PhysicalAction physicalAction = (PhysicalAction) action;
        updateReference(objectID, physicalAction.getFieldName(), physicalAction.getObject(), includeIDs);
      }
    }
  }

  private void updateReference(ObjectID objectID, String fieldName, Object value, BackReferences includeIDs) {
    if (LOCK_FIELD_NAME.equals(fieldName)) {
      lockField = (ObjectID) value;
      getListener().changed(objectID, null, lockField);
      includeIDs.addBackReference(lockField, objectID);
    }
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    writer.addPhysicalAction(LOCK_FIELD_NAME, lockField);
  }

  public String toString() {
    return "COWArrayListManagedStateObject(" + references + ")";
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    final int size = references.size();

    if (limit < 0) {
      limit = size;
    } else {
      limit = Math.min(limit, size);
    }

    Object[] data = new Object[limit];

    int index = 0;
    for (Iterator iter = references.iterator(); iter.hasNext() && index < limit; index++) {
      data[index] = iter.next();
    }

    return LogicalManagedObjectFacade.createListInstance(objectID, className, data, size);
  }

  public byte getType() {
    return COW_ARRAY_LIST_TYPE;
  }

  private void writeField(ObjectOutput out, String fieldName, Object fieldValue) throws IOException {
    out.writeUTF(fieldName);
    if (fieldValue == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      if (fieldValue instanceof ObjectID) {
        out.writeLong(((ObjectID) fieldValue).toLong());
      } else {
        out.writeObject(fieldValue);
      }
    }
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    writeField(out, LOCK_FIELD_NAME, lockField);
    super.basicWriteTo(out);
  }

  protected boolean basicEquals(LogicalManagedObjectState o) {
    COWArrayListManagedObjectState mo = (COWArrayListManagedObjectState) o;
    return references.equals(mo.references);
  }

  private static void readField(ObjectInput in, COWArrayListManagedObjectState listmo) throws IOException {
    String fieldName = in.readUTF();
    boolean fieldExist = in.readBoolean();
    if (fieldExist) {
      if (fieldName.equals(LOCK_FIELD_NAME)) {
        listmo.lockField = new ObjectID(in.readLong());
      } else {
        throw new AssertionError("Field not recognized in COWArrayListManagedObjectState.readFrom().");
      }
    }
  }

  static COWArrayListManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    COWArrayListManagedObjectState listmo = new COWArrayListManagedObjectState(in);
    readField(in, listmo);

    int size = in.readInt();
    ArrayList list = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      list.add(in.readObject());
    }
    listmo.references = list;
    return listmo;
  }

}