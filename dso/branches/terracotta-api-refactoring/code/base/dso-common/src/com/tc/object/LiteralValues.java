/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.dna.impl.ClassInstance;
import com.tc.object.dna.impl.ClassLoaderInstance;
import com.tc.object.dna.impl.EnumInstance;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsuible for handling literals
 */
public class LiteralValues implements ILiteralValues {
  private final Map values;

  public LiteralValues() {
    super();

    Map tmp = new HashMap();

    addMapping(tmp, Integer.class.getName(), INTEGER);
    addMapping(tmp, int.class.getName(), INTEGER);
    addMapping(tmp, Long.class.getName(), LONG);
    addMapping(tmp, long.class.getName(), LONG);
    addMapping(tmp, Character.class.getName(), CHARACTER);
    addMapping(tmp, char.class.getName(), CHARACTER);
    addMapping(tmp, Float.class.getName(), FLOAT);
    addMapping(tmp, float.class.getName(), FLOAT);
    addMapping(tmp, Double.class.getName(), DOUBLE);
    addMapping(tmp, double.class.getName(), DOUBLE);
    addMapping(tmp, Byte.class.getName(), BYTE);
    addMapping(tmp, byte.class.getName(), BYTE);
    addMapping(tmp, String.class.getName(), STRING);

    addMapping(tmp, UTF8ByteDataHolder.class.getName(), STRING_BYTES);

    addMapping(tmp, Short.class.getName(), SHORT);
    addMapping(tmp, short.class.getName(), SHORT);
    addMapping(tmp, Boolean.class.getName(), BOOLEAN);
    addMapping(tmp, boolean.class.getName(), BOOLEAN);

    addMapping(tmp, BigInteger.class.getName(), BIG_INTEGER);
    addMapping(tmp, BigDecimal.class.getName(), BIG_DECIMAL);

    addMapping(tmp, java.lang.Class.class.getName(), JAVA_LANG_CLASS);

    addMapping(tmp, ClassInstance.class.getName(), JAVA_LANG_CLASS_HOLDER);

    addMapping(tmp, ObjectID.class.getName(), OBJECT_ID);
    addMapping(tmp, StackTraceElement.class.getName(), STACK_TRACE_ELEMENT);

    addMapping(tmp, ClassLoaderInstance.class.getName(), JAVA_LANG_CLASSLOADER_HOLDER);

    addMapping(tmp, ENUM_CLASS_DOTS, ENUM);

    addMapping(tmp, EnumInstance.class.getName(), ENUM_HOLDER);

    addMapping(tmp, Currency.class.getName(), CURRENCY);

    values = Collections.unmodifiableMap(tmp);
  }

  public int valueFor(Object pojo) {
    if (pojo instanceof ClassLoader) { return JAVA_LANG_CLASSLOADER; }

    Class clazz = pojo.getClass();
    int i = valueForClassName(clazz.getName());
    if (i == OBJECT && ClassUtils.isEnum(pojo.getClass())) { return ENUM; }
    return i;
  }

  public boolean isLiteral(String className) {
    int i = valueForClassName(className);
    return i != OBJECT && i != ARRAY;
  }

  public boolean isLiteralInstance(Object obj) {
    if (obj == null) { return false; }
    int i = valueFor(obj);
    return i != OBJECT && i != ARRAY;
  }

  private static void addMapping(Map map, String className, int i) {
    Object prev = map.put(className, new Integer(i));
    Assert.assertNull(className, prev);
  }

  public int valueForClassName(String className) {
    if ((className != null) && className.startsWith("[")) { return ARRAY; }
    Integer i = (Integer) values.get(className);
    if (i == null) return OBJECT;
    return i.intValue();
  }
}
