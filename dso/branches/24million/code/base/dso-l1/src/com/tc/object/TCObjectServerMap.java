/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.TCServerMap;

public interface TCObjectServerMap extends TCObject {

  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param map ServerTCMap
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValueForKeyInMap(final TCServerMap map, final Object key);
  
  /**
   * Returns the size of a ServerTCMap
   * 
   * @param map ServerTCMap
   * 
   * @return int for size of map.
   */
  public int getSize(final TCServerMap map);
}