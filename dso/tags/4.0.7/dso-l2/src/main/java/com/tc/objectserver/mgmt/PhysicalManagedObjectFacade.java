/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.mgmt;

import com.tc.object.ObjectID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PhysicalManagedObjectFacade extends AbstractObjectFacade {
  private static final String[] EMPTY_STRING_ARRAY = new String[] {};

  private final String          className;
  private final Map             fields;
  private final ObjectID        objectID;
  private final ObjectID        parentID;
  private final boolean         isInner;
  private final int             arrayLength;
  private final boolean         isArray;
  private final String[]        fieldNames;

  public PhysicalManagedObjectFacade(ObjectID id, ObjectID parentID, String className, Map data, boolean isInner,
                                     int arrayLength, boolean isArray) {
    this.className = className;
    this.fields = Collections.unmodifiableMap(data);
    this.objectID = id;
    this.parentID = parentID;
    this.isInner = isInner;
    this.isArray = isArray;
    this.arrayLength = arrayLength;

    if (isArray) {
      this.fieldNames = (String[]) this.fields.keySet().toArray(EMPTY_STRING_ARRAY);
    } else {
      this.fieldNames = sortFieldNames((String[]) this.fields.keySet().toArray(EMPTY_STRING_ARRAY));
    }
  }

  private String[] sortFieldNames(String[] names) {
    List special = new ArrayList();
    List regular = new ArrayList();

    for (String name : names) {
      if (name.indexOf('$') >= 0) {
        special.add(name);
      } else {
        regular.add(name);
      }
    }

    String[] first = (String[]) special.toArray(EMPTY_STRING_ARRAY);
    String[] second = (String[]) regular.toArray(EMPTY_STRING_ARRAY);
    Arrays.sort(first);
    Arrays.sort(second);

    String[] rv = new String[names.length];
    System.arraycopy(first, 0, rv, 0, first.length);
    System.arraycopy(second, 0, rv, first.length, second.length);

    return rv;
  }

  @Override
  public String getClassName() {
    return this.className;
  }

  @Override
  public String[] getFields() {
    return this.fieldNames.clone();
  }

  @Override
  protected Object basicGetFieldValue(String fieldName) {
    checkValidName(fieldName);
    return this.fields.get(fieldName);
  }

  @Override
  public boolean isPrimitive(String fieldName) {
    Object value = getFieldValue(fieldName);
    return !(value instanceof ObjectID);
  }

  @Override
  public ObjectID getObjectId() {
    return this.objectID;
  }

  @Override
  public boolean isInnerClass() {
    return this.isInner;
  }

  @Override
  public ObjectID getParentObjectId() {
    if (!this.isInner) { throw new IllegalStateException("Not an inner class"); }
    return this.parentID;
  }

  @Override
  public boolean isArray() {
    return this.isArray;
  }

  @Override
  public int getArrayLength() {
    if (!this.isArray) { throw new IllegalStateException("Not an array"); }
    return this.arrayLength;
  }

  @Override
  public boolean isList() {
    return false;
  }

  @Override
  public boolean isSet() {
    return false;
  }

  @Override
  public boolean isMap() {
    return false;
  }

  @Override
  public int getFacadeSize() {
    throw new UnsupportedOperationException("Not a collection");
  }

  @Override
  public int getTrueObjectSize() {
    throw new UnsupportedOperationException("Not a collection");
  }

  private void checkValidName(String fieldName) {
    if (this.fields.containsKey(fieldName)) { return; }
    throw new IllegalArgumentException(className + "." + fieldName + " does not exist");
  }

}
