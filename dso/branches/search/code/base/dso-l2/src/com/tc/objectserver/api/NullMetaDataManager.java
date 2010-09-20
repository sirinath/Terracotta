/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.dna.api.MetaDataReader;

public class NullMetaDataManager implements MetaDataManager {

  public void processMetaData(MetaDataReader reader) {
    //Do nothing, since no relevant oss metadata.
  }

}
