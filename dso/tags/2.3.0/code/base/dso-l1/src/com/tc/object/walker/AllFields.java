/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class AllFields {
  private static final Class OBJECT = Object.class;

  private final Set          fields = new TreeSet();

  Iterator getFields() {
    return Collections.unmodifiableSet(fields).iterator();
  }

  static AllFields getAllFields(Object o) {
    // XXX: cache?

    AllFields allFields = new AllFields();

    Map fieldNames = new HashMap();

    Class c = o.getClass();
    while (c != OBJECT) {
      Field[] fields = c.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        Field f = fields[i];

        if (Modifier.isStatic(f.getModifiers())) {
          continue;
        }

        FieldData key = new FieldData(f);

        FieldData prev = (FieldData) fieldNames.put(f.getName(), key);
        if (prev != null) {
          key.setShadowed(true);
          prev.setShadowed(true);
        }

        allFields.fields.add(key);
      }

      c = c.getSuperclass();
    }

    return allFields;

  }

}