/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.io.TCDataInput;

public interface Decompressor {

  Object readCompressed(TCDataInput compressedInput);
  
}
