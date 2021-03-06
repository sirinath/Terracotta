/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.ClassCatalog;

public class SleepycatSerializationAdapterFactory implements SerializationAdapterFactory {

  public SerializationAdapter newAdapter(ClassCatalog classCatalog) {
    return new SleepycatSerializationAdapter(classCatalog);
  }

}
