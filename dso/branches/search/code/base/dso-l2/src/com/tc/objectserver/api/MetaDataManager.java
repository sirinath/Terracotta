/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.dna.api.MetaDataReader;

/**
 * Manager to process Metadata from a DNA
 * 
 * @Nabib El-Rahman
 */
public interface MetaDataManager {
  
  /**
   * Process metadata.
   * 
   * @param MetaDataReader metadata reader associated with a DNA.
   * 
   */
  public void processMetaData(MetaDataReader reader);

}
