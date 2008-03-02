/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.io.TCDataOutput;

public interface Compressor {
  
  void writeCompressed(Object object, TCDataOutput output);

}
