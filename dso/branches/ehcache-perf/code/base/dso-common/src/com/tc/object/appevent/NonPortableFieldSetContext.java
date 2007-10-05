/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

public class NonPortableFieldSetContext extends NonPortableEventContext {

  private static final long serialVersionUID = -556002400100752262L;

  private final String           fieldName;
  private transient final Object fieldValue;

  public static final String     FIELD_NAME_LABEL = "Non-portable field name";

  public NonPortableFieldSetContext(String threadName, String clientId, Object pojo, String fieldName, Object fieldValue) {
    super(pojo, threadName, clientId);
    this.fieldName = fieldName;
    this.fieldValue = fieldValue;
  }

  public String getFieldName() {
    return fieldName;
  }

  public Object getFieldValue() {
    return fieldValue;
  }

  public void addDetailsTo(NonPortableReason reason) {
    super.addDetailsTo(reason);
    reason.addDetail(FIELD_NAME_LABEL, fieldName);
  }

}
