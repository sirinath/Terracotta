/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.objectserver.mgmt.FacadeUtil;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.objectserver.mgmt.MapEntryFacadeImpl;
import com.tc.objectserver.persistence.sleepycat.PersistableCollection;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * state for maps
 */
public class MapManagedObjectState extends LogicalManagedObjectState implements PrettyPrintable, PersistableObjectState {
  protected Map references;

  protected MapManagedObjectState(long classID, Map map) {
    super(classID);
    this.references = map;
  }

  protected MapManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();
      applyMethod(objectID, includeIDs, method, params);
    }
  }

  protected void applyMethod(ObjectID objectID, BackReferences includeIDs, int method, Object[] params) {
    switch (method) {
      case SerializationUtil.PUT:

        mapPreProcess(params);
        Object key = getKey(params);
        Object value = getValue(params);
        this.references.put(key, value);
        if (key instanceof ObjectID) {
          ObjectID v = (ObjectID) key;
          getListener().changed(objectID, null, v);
          addBackReferenceForKey(includeIDs, v, objectID);
        }
        if (value instanceof ObjectID) {
          ObjectID v = (ObjectID) value;
          getListener().changed(objectID, null, v);
          addBackReferenceForValue(includeIDs, v, objectID);
        }
        break;
      case SerializationUtil.REMOVE:
        this.references.remove(params[0]);
        break;
      case SerializationUtil.CLEAR:
        this.references.clear();
        break;
      default:
        throw new AssertionError("Invalid action:" + method);
    }

  }

  protected void addBackReferenceForKey(BackReferences includeIDs, ObjectID key, ObjectID map) {
    includeIDs.addBackReference(key, map);
  }

  protected void addBackReferenceForValue(BackReferences includeIDs, ObjectID value, ObjectID map) {
    includeIDs.addBackReference(value, map);
  }

  private Object getKey(Object[] params) {
    // Hack hack big hack for trove maps which replace the key on set as opposed to HashMaps which do not.
    return params.length == 3 ? params[1] : params[0];
  }

  private Object getValue(Object[] params) {
    // Hack hack big hack for trove maps which replace the key on set as opposed to HashMaps which do not.
    return params.length == 3 ? params[2] : params[1];
  }

  private void mapPreProcess(Object[] params) {
    // Hack hack big hack for trove maps which replace the key on set as opposed to HashMaps which do not.
    if (params.length == 3) {
      this.references.remove(params[0]);
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    for (Iterator i = this.references.entrySet().iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Object key = entry.getKey();
      Object value = entry.getValue();
      writer.addLogicalAction(SerializationUtil.PUT, new Object[] { key, value });
    }
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    addAllObjectReferencesFromIteratorTo(this.references.keySet().iterator(), refs);
    addAllObjectReferencesFromIteratorTo(this.references.values().iterator(), refs);
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out = out.println("MapManagedObjectState").duplicateAndIndent();
    out.indent().println("references: " + this.references);
    return rv;
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    final int size = this.references.size();

    if (limit < 0) {
      limit = size;
    } else {
      limit = Math.min(limit, size);
    }

    MapEntryFacade[] data = new MapEntryFacade[limit];

    int index = 0;

    for (Iterator i = this.references.entrySet().iterator(); i.hasNext() && index < limit; index++) {
      Entry entry = (Entry) i.next();
      Object key = FacadeUtil.processValue(entry.getKey());
      Object value = FacadeUtil.processValue(entry.getValue());
      data[index] = new MapEntryFacadeImpl(key, value);
    }

    return LogicalManagedObjectFacade.createMapInstance(objectID, className, data, size);
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    // CollectionsPersistor will save retrieve data in references map.
    if (false) { throw new IOException(); }
  }

  public void setMap(Map map) {
    if (this.references != null) { throw new AssertionError("The references map is already set ! " + this.references); }
    this.references = map;
  }

  public Map getMap() {
    return this.references;
  }

  // CollectionsPersistor will save retrieve data in references map.
  static MapManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    if (false) {
      // This is added to make the compiler happy. For some reason if I have readFrom() method throw
      // ClassNotFoundException in LinkedHashMapManagedObjectState, it shows as an error !!
      throw new ClassNotFoundException();
    }
    return new MapManagedObjectState(in);
  }

  public byte getType() {
    return MAP_TYPE;
  }

  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    MapManagedObjectState mmo = (MapManagedObjectState) o;
    return this.references.equals(mmo.references);
  }

  public PersistableCollection getPersistentCollection() {
    return (PersistableCollection) getMap();
  }

  public void setPersistentCollection(PersistableCollection collection) {
    setMap((Map) collection);
  }
}