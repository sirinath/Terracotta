/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.managedobject.bytecode.ClassNotCompatableException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * State for Physically managed objects. This class is abstract and generated classes of Physical State Class Loader
 * extends this class So changes to this class needs to be done cautiously.
 */
public abstract class PhysicalManagedObjectState extends AbstractManagedObjectState implements Serializable,
    PrettyPrintable {

  private static final TCLogger logger = TCLogging.getLogger(PhysicalManagedObjectState.class);

  public PhysicalManagedObjectState() {
    super();
  }

  /**
   * This is only for testing, its highly inefficient
   */
  protected boolean basicEquals(AbstractManagedObjectState o) {
    PhysicalManagedObjectState cmp = (PhysicalManagedObjectState) o;
    boolean result = getParentID().equals(cmp.getParentID()) && getClassName().equals(cmp.getClassName())
                     && getLoaderDescription().equals(cmp.getLoaderDescription());
    if (!result) return result;
    Map mine = addValues(new HashMap());
    Map his = cmp.addValues(new HashMap());
    return mine.equals(his);
  }

  public ObjectID getParentID() {
    return ObjectID.NULL_ID;
  }

  public void setParentID(ObjectID id) {
    // This is over-riden when needed
  }

  public int hashCode() {
    throw new TCRuntimeException("Don't hash me!");
  }

  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    ManagedObjectChangeListener listener = getListener();
    while (cursor.next()) {
      PhysicalAction a = cursor.getPhysicalAction();
      Object value = a.getObject();
      Object old = set(a.getFieldName(), value);
      ObjectID oldValue = old instanceof ObjectID ? (ObjectID) old : ObjectID.NULL_ID;
      ObjectID newValue = value instanceof ObjectID ? (ObjectID) value : ObjectID.NULL_ID;
      listener.changed(objectID, oldValue, newValue);
    }
  }

  /**
   * @return old Value
   */
  public Object set(String fieldName, Object value) {
    try {
      return basicSet(fieldName, value);
    } catch (ClassNotCompatableException cne) {
      // This exception triggers a regeneration of the state class !
      logger.warn("Recoverable Incompatable Class Change Identified : " + cne.getMessage());
      throw cne;
    } catch (ClassCastException cce) {
      // This is due to a change in the type of the fields which is currently not supported.
      // Not throwing the exception 'coz we dont want to crash the server because of an
      // incompatable change
      cce.printStackTrace();
      logger.error("Unrecoverable Incompatable Class Change : fieldName = " + fieldName + " value = " + value, cce);
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Incompatable Change : Class Does not support it", e);
      return null;
    }
  }
  
  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  /**
   * This method is generated by PhysicalStateClassLoader. It adds all the values of the fields into the map. This is
   * just a convinent methods for printing the values, creating facade and checking for equals, but this should not be
   * used in any other case as there is an overhead involved.
   */
  public abstract Map addValues(Map m);

  public abstract Set getObjectReferences();

  /**
   * This method is generated by PhysicalStateClassLoader.
   */
  protected abstract void basicDehydrate(DNAWriter writer);

  /**
   * This method is generated by PhysicalStateClassLoader.
   */
  protected abstract int getClassId();

  /**
   * This method is generated by PhysicalStateClassLoader.
   * 
   * @return old Value
   */
  protected abstract Object basicSet(String fieldName, Object value);

  /**
   * This method is generated by PhysicalStateClassLoader.
   */
  protected abstract void readObject(ObjectInput in) throws IOException, ClassNotFoundException;

  /**
   * This method is generated by PhysicalStateClassLoader.
   * 
   * @return old Value
   */
  protected abstract void writeObject(ObjectOutput out) throws IOException;

  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    basicDehydrate(writer);
    writer.setParentObjectID(getParentID());
  }

  public String toString() {
    // XXX: Um... this is gross.
    StringWriter writer = new StringWriter();
    PrintWriter pWriter = new PrintWriter(writer);
    new PrettyPrinter(pWriter).visit(this);
    return writer.getBuffer().toString();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out = out.print(getClass().getName()).duplicateAndIndent().println();
    out.indent().print("parentID  : " + getParentID());
    out.indent().print("className : " + getClassName());
    out.indent().print("loaderDesc: " + getLoaderDescription());
    out.indent().print("references: " + addValues(new HashMap())).println();
    out.indent().print("listener: " + getListener()).println();
    return rv;
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    // NOTE: limit is ignored for physical object facades

    Map dataCopy = addValues(new HashMap());

    ObjectID parentID = getParentID();
    boolean isInner = !parentID.isNull();

    return new PhysicalManagedObjectFacade(objectID, parentID, className, dataCopy, isInner, DNA.NULL_ARRAY_SIZE, false);
  }

  public byte getType() {
    return PHYSICAL_TYPE;
  }

  public void writeTo(ObjectOutput out) throws IOException {
    // write the class identifier
    out.writeInt(this.getClassId());
    ObjectID parentID = getParentID();
    if (ObjectID.NULL_ID.equals(parentID)) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      out.writeLong(parentID.toLong());
    }

    writeObject(out);
  }

  static PhysicalManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    // read the class identifier
    int classId = in.readInt();
    ObjectID pid = ObjectID.NULL_ID;
    if (in.readBoolean()) {
      pid = new ObjectID(in.readLong());
    }

    PhysicalManagedObjectState pmos = ManagedObjectStateFactory.getInstance().createPhysicalState(pid, classId);
    pmos.readObject(in);
    return pmos;
  }

}
