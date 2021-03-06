/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.bulkload;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;

public final class BulkLoadConstants {

  private final TCProperties  tcProperties;

  public BulkLoadConstants(TCProperties tcProperties) {
    this.tcProperties = tcProperties;
  }

  public boolean isLoggingEnabled() {
    return tcProperties.getBoolean(TCPropertiesConsts.TOOLKIT_BULKLOAD_LOGGING_ENABLED);
     
  }

  public int getBatchedPutsBatchBytes() {
    return tcProperties.getInt(TCPropertiesConsts.TOOLKIT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE);
  }

  public long getBatchedPutsBatchTimeMillis() {
    return tcProperties.getLong(TCPropertiesConsts.TOOLKIT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS);
  }

  public int getBatchedPutsThrottlePutsAtByteSize() {
    return tcProperties.getInt(TCPropertiesConsts.TOOLKIT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE);
  }

}
