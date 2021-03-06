/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.io.serializer.TCCustomByteArrayOutputStream;

public interface PersistenceTransaction {

  public Object getProperty(Object key);

  public Object setProperty(Object key, Object value);

  public void commit();
  
  public void setBuffer(TCCustomByteArrayOutputStream buffer);
}
