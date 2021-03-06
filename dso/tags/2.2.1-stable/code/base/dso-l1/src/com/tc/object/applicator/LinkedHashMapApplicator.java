/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.util.Assert;
import com.tc.util.FieldUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;

/**
 * Apply a logical action to an object
 */
public class LinkedHashMapApplicator extends PartialHashMapApplicator {
  private static final String ACCESS_ORDER_FIELDNAME = "java.util.LinkedHashMap.accessOrder";
  private static final Field  ACCESS_ORDER_FIELD;

  static {
    try {
      ACCESS_ORDER_FIELD = LinkedHashMap.class.getDeclaredField("accessOrder");
      ACCESS_ORDER_FIELD.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public LinkedHashMapApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object pojo) throws IOException,
      ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        PhysicalAction physicalAction = (PhysicalAction) action;
        Assert.assertEquals(ACCESS_ORDER_FIELDNAME, physicalAction.getFieldName());
        setAccessOrder(pojo, physicalAction.getObject());
      } else {
        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        apply(objectManager, pojo, method, params);
      }
    }
  }

  private void setAccessOrder(Object target, Object value) {
    try {
      FieldUtils.tcSet(target, value, ACCESS_ORDER_FIELD);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void apply(ClientObjectManager objectManager, Object pojo, int method, Object[] params) {
    switch (method) {
      case SerializationUtil.GET:
        ((LinkedHashMap) pojo).get(params[0]);
        break;
      default:
        super.apply(objectManager, pojo, method, params);
    }
  }

  private boolean getAccessOrder(Object pojo) {
    try {
      return ACCESS_ORDER_FIELD.getBoolean(pojo);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    writer.addPhysicalAction(ACCESS_ORDER_FIELDNAME, new Boolean(getAccessOrder(pojo)));
    super.dehydrate(objectManager, tcObject, writer, pojo);
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    if (!cursor.next(encoding)) { throw new AssertionError(
                                                           "Cursor is empty in LinkedHashMapApplicator.getNewInstance()"); }
    PhysicalAction physicalAction = cursor.getPhysicalAction();

    return new LinkedHashMap(1, 0.75f, ((Boolean) physicalAction.getObject()).booleanValue());
  }
}
