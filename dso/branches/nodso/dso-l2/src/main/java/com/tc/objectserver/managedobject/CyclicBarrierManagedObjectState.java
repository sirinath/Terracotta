/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

public class CyclicBarrierManagedObjectState extends LogicalManagedObjectState {
  public static final String    CYCLE_BARRIER_CLASSNAME = "com.terracotta.coordination.BarrierImpl";
  private static final TCLogger logger                  = TCLogging.getLogger(CyclicBarrierManagedObjectState.class);

  private static String         PARTIES                 = "parties";
  private static String         COUNT                   = "count";

  private volatile int          parties;
  private volatile int          count;

  public CyclicBarrierManagedObjectState(final long classID) {
    super(classID);
    logger.info("abhim state classID cons " + classID);
  }

  public CyclicBarrierManagedObjectState(ObjectInput in) throws IOException {
    super(in);
    parties = in.readInt();
    count = in.readInt();
    logger.info("abhim state cons " + count);
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo applyInfo) throws IOException {
    String fieldName;
    Object fieldValue;
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        final PhysicalAction pAction = (PhysicalAction) action;
        fieldName = pAction.getFieldName();
        fieldValue = pAction.getObject();
        if (fieldName.equals(PARTIES)) {
          parties = ((Integer) fieldValue).intValue();
        }
        if (fieldName.equals(COUNT)) {
          count = ((Integer) fieldValue).intValue();
        }
        logger.info("abhim state apply PhysicalAction " + count + " " + parties);
      } else {
        final LogicalAction logicalAction = (LogicalAction) action;
        final int method = logicalAction.getMethod();
        final Object[] params = logicalAction.getParameters();
        applyMethod(objectID, applyInfo, method, params);
      }
    }
  }

  protected void applyMethod(final ObjectID objectID, final ApplyTransactionInfo applyInfo, final int method,
                             final Object[] params) {
    switch (method) {
      case SerializationUtil.ADD:
        --count;
        logger.info("abhim state method " + count + " " + objectID);
        if (count == 0) {
          reset();
        }
        break;
      default:
        throw new IllegalArgumentException("method " + method + " not defined for CyclicBarrierManagedObjectState");
    }
  }

  private void reset() {
    count = parties;
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    logger.info("abhim state dehydrate");
    writer.addPhysicalAction(PARTIES, parties);
    writer.addPhysicalAction(COUNT, count);
  }

  @Override
  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    return null;
  }

  @Override
  public byte getType() {
    return CYCLE_BARRIER_TYPE;
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeInt(parties);
    out.writeInt(count);
  }

  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    CyclicBarrierManagedObjectState other = (CyclicBarrierManagedObjectState) o;
    return (other.parties == parties) && (other.count == count);
  }

  static CyclicBarrierManagedObjectState readFrom(ObjectInput in) throws IOException {
    logger.info("abhim state readFrom");
    CyclicBarrierManagedObjectState state = new CyclicBarrierManagedObjectState(in);
    return state;
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    //
  }
}
